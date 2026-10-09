package pe.edu.upc.healthify.features.main.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetActiveTargetsUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.main.presentation.state.HomeFollowUp
import pe.edu.upc.healthify.features.monitoring.application.usecase.AcknowledgeConsistencyPromptUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.DetectLoggingGapUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetConsistencyIndexUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetTodayProgressUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyState
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.testing.FakeActiveTargetsRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePatientMonitoringRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.activeTargets
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.readStatus
import pe.edu.upc.healthify.testing.summary
import java.time.Instant
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedDiaryEntriesUseCase
import pe.edu.upc.healthify.testing.FakeDiaryRepository

class PatientHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = FakeSessionRepository()
    private val targets = FakeActiveTargetsRepository()
    private val monitoring = FakePatientMonitoringRepository()
    private val careLinks = FakeCareLinkRepository()
    private val diary = FakeDiaryRepository()
    private val connectivity = FakeConnectivityObserver()

    private val viewModel by lazy {
        PatientHomeViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(session),
            getActiveTargets = GetActiveTargetsUseCase(targets),
            getTodayProgress = GetTodayProgressUseCase(monitoring, fixedClock),
            getNextFollowUp = GetNextFollowUpUseCase(monitoring),
            getConsistencyIndex = GetConsistencyIndexUseCase(monitoring),
            acknowledgeConsistencyPrompt = AcknowledgeConsistencyPromptUseCase(monitoring),
            detectLoggingGap = DetectLoggingGapUseCase(monitoring, fixedClock),
            getTargetsReadStatus = GetTargetsReadStatusUseCase(careLinks),
            observeSyncedDiaryEntries = ObserveSyncedDiaryEntriesUseCase(diary),
            clock = fixedClock,
            connectivityObserver = connectivity,
        )
    }

    @Test
    fun `PT3 shows the targets, today's energy, how I'm doing and the next consultation`() {
        val scheduled = Instant.parse("2026-10-09T15:00:00Z")
        monitoring.nextFollowUpResult = Result.success(NextFollowUp(FollowUpId(7), scheduled))

        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertEquals("María", state.greetingName)
        assertEquals("M", state.avatarInitial)
        assertEquals(1850.0, state.targets?.energyKcal ?: 0.0, 0.0)
        assertEquals(1260.0, state.consumedKcal ?: 0.0, 0.0)
        assertEquals(ComplianceOutcome.MET, state.todayOutcome)
        assertTrue(state.howAmIDoingVisible)
        assertEquals(HomeFollowUp(7, scheduled), state.nextFollowUp)
        assertTrue(state.nextFollowUpVisible)
        assertNull(state.cachedAt)
        assertFalse(state.consistencyCardVisible)
        assertFalse(state.loggingGapCardVisible)
        assertFalse(state.compactRegisterButton)
    }

    @Test
    fun `queued meals accepted by the backend update today's energy`() {
        val withMeals = monitoring.progressResult
        monitoring.progressResult = Result.success(DailyProgress.unlogged(TODAY))
        assertEquals(0.0, viewModel.state.value.consumedKcal ?: -1.0, 0.0)

        monitoring.progressResult = withMeals
        diary.synced.tryEmit(Unit)

        assertEquals(1260.0, viewModel.state.value.consumedKcal ?: 0.0, 0.0)
        assertEquals(ComplianceOutcome.MET, viewModel.state.value.todayOutcome)
    }

    @Test
    fun `a day without entries is an invitation, not a failure`() {
        monitoring.progressResult = Result.success(DailyProgress.unlogged(TODAY))

        val state = viewModel.state.value

        assertEquals(ComplianceOutcome.UNLOGGED, state.todayOutcome)
        assertEquals(0.0, state.consumedKcal ?: -1.0, 0.0)
    }

    @Test
    fun `PT3_O offline shows the targets saved on the phone and hides what lives in Monitoring`() {
        val savedAt = Instant.parse("2026-10-07T08:10:00Z")
        connectivity.online.value = false
        targets.result = Result.success(ActiveTargetsLookup(activeTargets(), fromCache = true, savedAt = savedAt))
        monitoring.progressResult = failureOf(DomainError.Network)
        monitoring.consistencyResult = Result.success(ConsistencyIndex(ConsistencyState.ALERT, patientPromptPending = true))
        monitoring.nextFollowUpResult = Result.success(NextFollowUp(FollowUpId(7), Instant.parse("2026-10-09T15:00:00Z")))

        val state = viewModel.state.value

        assertTrue(state.isOffline)
        assertEquals(1850.0, state.targets?.energyKcal ?: 0.0, 0.0)
        assertEquals(savedAt, state.cachedAt)
        assertNull(state.consumedKcal)
        assertFalse(state.howAmIDoingVisible)
        assertFalse(state.nextFollowUpVisible)
        // PT16: sin conexión la tarjeta «Algo no cuadra» simplemente no aparece (y no se acusa).
        assertFalse(state.consistencyCardVisible)
    }

    @Test
    fun `PT3_V without published targets shows the empty state`() {
        targets.result = Result.success(ActiveTargetsLookup.None)

        val state = viewModel.state.value

        assertTrue(state.hasNoTargets)
        assertNull(state.targets)
        assertFalse(state.isLoading)
    }

    @Test
    fun `an error without a saved copy uses the same copy as the empty state`() {
        targets.result = failureOf(DomainError.Unexpected("HTTP_500"))

        assertTrue(viewModel.state.value.hasNoTargets)
    }

    @Test
    fun `with an active consistency alert the card shows and is acknowledged once when painted`() {
        monitoring.consistencyResult = Result.success(ConsistencyIndex(ConsistencyState.ALERT, patientPromptPending = true))

        val state = viewModel.state.value
        assertTrue(state.consistencyCardVisible)
        assertTrue(state.compactRegisterButton)

        viewModel.onConsistencyCardShown()
        viewModel.onConsistencyCardShown()

        assertEquals(1, monitoring.acknowledgements)
    }

    @Test
    fun `an alert already seen keeps the card but is not acknowledged again`() {
        monitoring.consistencyResult = Result.success(ConsistencyIndex(ConsistencyState.ALERT, patientPromptPending = false))

        assertTrue(viewModel.state.value.consistencyCardVisible)
        viewModel.onConsistencyCardShown()

        assertEquals(0, monitoring.acknowledgements)
    }

    @Test
    fun `a failed acknowledgement is retried the next time the card is painted`() {
        monitoring.consistencyResult = Result.success(ConsistencyIndex(ConsistencyState.ALERT, patientPromptPending = true))
        monitoring.acknowledgeResult = failureOf(DomainError.Network)
        viewModel.onConsistencyCardShown()

        monitoring.acknowledgeResult = Result.success(Unit)
        viewModel.onConsistencyCardShown()
        viewModel.onConsistencyCardShown()

        assertEquals(2, monitoring.acknowledgements)
    }

    @Test
    fun `without a consistency index there is no card`() {
        monitoring.consistencyResult = Result.success(null)

        assertFalse(viewModel.state.value.consistencyCardVisible)
    }

    @Test
    fun `PT3_M offers the new version once and Later hides it`() {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = 4))

        assertEquals(4, viewModel.state.value.targetsChangedVersion)
        assertTrue(viewModel.state.value.targetsChangedSheetVisible)

        viewModel.onTargetsChangedDismissed()
        viewModel.onScreenResumed() // primera reanudación: ya cargó init
        viewModel.onScreenResumed() // vuelve a Inicio: refresca, pero «Más tarde» ya descartó la v4

        assertNull(viewModel.state.value.targetsChangedVersion)
    }

    @Test
    fun `PT3_M is not offered for the first version`() {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = 1))

        assertNull(viewModel.state.value.targetsChangedVersion)
    }

    @Test
    fun `PT18 invites to log after three days without entries`() {
        monitoring.summaryResult = Result.success(summary(loggedDays = 0))
        monitoring.progressResult = Result.success(DailyProgress.unlogged(TODAY))

        val state = viewModel.state.value

        assertTrue(state.loggingGapCardVisible)
        assertTrue(state.compactRegisterButton)
    }

    @Test
    fun `PT18 is not shown when targets were published less than three days ago`() {
        monitoring.summaryResult = Result.success(summary(loggedDays = 0))
        targets.result = Result.success(ActiveTargetsLookup(activeTargets(validFrom = Instant.parse("2026-10-06T12:00:00Z"))))

        assertFalse(viewModel.state.value.loggingGapCardVisible)
        assertTrue(monitoring.summaryRanges.isEmpty())
    }

    @Test
    fun `coming back online refreshes Home`() {
        connectivity.online.value = false
        targets.result = Result.success(ActiveTargetsLookup(activeTargets(), fromCache = true, savedAt = Instant.EPOCH))
        viewModel.state.value
        val callsOffline = targets.calls

        targets.result = Result.success(ActiveTargetsLookup(activeTargets()))
        connectivity.online.value = true

        assertEquals(callsOffline + 1, targets.calls)
        assertNull(viewModel.state.value.cachedAt)
        assertFalse(viewModel.state.value.isOffline)
    }
}
