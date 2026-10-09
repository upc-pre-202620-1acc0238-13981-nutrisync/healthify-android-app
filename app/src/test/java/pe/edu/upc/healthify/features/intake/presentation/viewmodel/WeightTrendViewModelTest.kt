package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ConsumeSelfWeighInSavedNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetWeightTrendUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSelfWeighInSavedNoticeUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.presentation.state.WeeklyCard
import pe.edu.upc.healthify.features.intake.presentation.state.WeightTrendEvent
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetLatestWeeklySummaryUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSelfWeighInRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.FakeWeeklySummaryRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.weightTrend
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedSelfWeighInsUseCase

class WeightTrendViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val weighIns = FakeSelfWeighInRepository()
    private val weekly = FakeWeeklySummaryRepository()
    private val connectivity = FakeConnectivityObserver()

    private val viewModel by lazy {
        WeightTrendViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetWeightTrendUseCase(weighIns),
            GetLatestWeeklySummaryUseCase(weekly),
            ObserveSelfWeighInSavedNoticeUseCase(weighIns),
            ConsumeSelfWeighInSavedNoticeUseCase(weighIns),
            ObserveSyncedSelfWeighInsUseCase(weighIns),
            connectivity,
        )
    }

    @Test
    fun `PT13 shows the smoothed trend and the weekly card`() {
        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertEquals(6, state.trend?.rangePoints?.size)
        assertFalse(state.isEmpty)
        assertFalse(state.showExcluded)
        assertEquals(WeeklyCard.Ready("Cumpliste tus metas 5 de 7 días."), state.weeklyCard)
    }

    @Test
    fun `PT13 reads the trend again when queued weigh-ins are accepted`() {
        weighIns.trendResult = Result.success(null)
        assertTrue(viewModel.state.value.isEmpty)
        val before = weighIns.trendRequests

        weighIns.trendResult = Result.success(weightTrend())
        weighIns.synced.tryEmit(Unit)

        assertEquals(before + 1, weighIns.trendRequests)
        assertEquals(6, viewModel.state.value.trend?.rangePoints?.size)
    }

    @Test
    fun `PT13_V with no trend yet (404) is the empty invitation, not an error`() {
        weighIns.trendResult = Result.success(null)

        val state = viewModel.state.value

        assertTrue(state.isEmpty)
        assertNull(state.trend)
        assertFalse(state.loadFailed)
    }

    @Test
    fun `PT13_V also when the range has fewer than two points`() {
        weighIns.trendResult = Result.success(weightTrend(days = 1))

        assertTrue(viewModel.state.value.isEmpty)
        assertNull(viewModel.state.value.trend)
    }

    @Test
    fun `readings outside the protocol show the excluded notice`() {
        weighIns.trendResult = Result.success(weightTrend(excluded = 3))

        assertTrue(viewModel.state.value.showExcluded)
    }

    @Test
    fun `without connection the trend comes from the phone and says since when`() {
        connectivity.online.value = false
        weighIns.trendResult = Result.success(weightTrend(fromCache = true))

        val state = viewModel.state.value

        assertTrue(state.isOffline)
        val trend = requireNotNull(state.trend)
        assertTrue(trend.fromCache)
        assertEquals(trend.savedAt, state.cachedAt)
    }

    @Test
    fun `without connection and without a copy there is an offline state, not an error`() {
        connectivity.online.value = false
        weighIns.trendResult = failureOf(DomainError.Network)
        weekly.result = failureOf(DomainError.Network)

        val state = viewModel.state.value

        assertTrue(state.isOfflineWithoutCopy)
        assertNull(state.weeklyCard)
    }

    @Test
    fun `a server error offers a retry that loads again`() {
        weighIns.trendResult = failureOf(DomainError.Unexpected("HTTP_500"))
        assertTrue(viewModel.state.value.loadFailed)

        weighIns.trendResult = Result.success(weightTrend())
        viewModel.onRetry()

        assertFalse(viewModel.state.value.loadFailed)
        assertEquals(6, viewModel.state.value.trend?.points?.size)
    }

    @Test
    fun `the weekly card invites while there is no summary and hides when the function is off`() {
        weekly.result = Result.success(WeeklySummaryAvailability.NotYet)
        assertEquals(WeeklyCard.NotYet, viewModel.state.value.weeklyCard)

        weekly.result = Result.success(WeeklySummaryAvailability.Off)
        viewModel.onRetry()
        assertNull(viewModel.state.value.weeklyCard)
    }

    @Test
    fun `a saved weigh-in shows the snackbar once and reloads the trend`() = runTest {
        viewModel.events.test {
            val before = weighIns.trendRequests
            weighIns.publishNotice(SelfWeighInOutcome.QUEUED)

            assertEquals(WeightTrendEvent.ShowSaved(SelfWeighInOutcome.QUEUED), awaitItem())
            assertTrue(weighIns.trendRequests > before)
            weighIns.publishNotice(SelfWeighInOutcome.RECORDED)
            assertEquals(WeightTrendEvent.ShowSaved(SelfWeighInOutcome.RECORDED), awaitItem())
            expectNoEvents()
        }
    }
}
