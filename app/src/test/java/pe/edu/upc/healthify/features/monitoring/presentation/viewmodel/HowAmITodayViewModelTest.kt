package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetTodayProgressUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePatientMonitoringRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock

class HowAmITodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monitoring = FakePatientMonitoringRepository()
    private val connectivity = FakeConnectivityObserver()

    private val viewModel by lazy {
        HowAmITodayViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetTodayProgressUseCase(monitoring, fixedClock),
            connectivity,
        )
    }

    @Test
    fun `PT15 shows today's outcome`() {
        assertEquals(ComplianceOutcome.MET, viewModel.state.value.outcome)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `PT15 with nothing logged is the empty invitation, not an error`() {
        monitoring.progressResult = Result.success(DailyProgress.unlogged(TODAY))

        assertEquals(ComplianceOutcome.UNLOGGED, viewModel.state.value.outcome)
        assertFalse(viewModel.state.value.loadFailed)
    }

    @Test
    fun `PT15_O without connection shows the offline state`() {
        monitoring.progressResult = failureOf(DomainError.Network)

        assertTrue(viewModel.state.value.isOffline)
        assertNull(viewModel.state.value.outcome)
        assertFalse(viewModel.state.value.loadFailed)
    }

    @Test
    fun `a server error offers a retry that loads again`() {
        monitoring.progressResult = failureOf(DomainError.Unexpected("HTTP_500"))
        assertTrue(viewModel.state.value.loadFailed)

        monitoring.progressResult = Result.success(DailyProgress(TODAY, ComplianceOutcome.SHORT, 900.0))
        viewModel.onRetry()

        assertEquals(ComplianceOutcome.SHORT, viewModel.state.value.outcome)
        assertFalse(viewModel.state.value.loadFailed)
    }
}
