package pe.edu.upc.healthify.features.monitoring

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.application.usecase.AcknowledgeConsistencyPromptUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.DetectLoggingGapUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyState
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.LoggingGap
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDailyProgress
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceDto
import pe.edu.upc.healthify.testing.FakePatientMonitoringRepository
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.summary

class MonitoringTest {

    private val repository = FakePatientMonitoringRepository()

    @Test
    fun `an unevaluated day is unlogged with no energy, never a failure`() {
        val progress = emptyList<DailyComplianceDto>().toDailyProgress(TODAY)

        assertEquals(DailyProgress.unlogged(TODAY), progress)
        assertFalse(progress.isLogged)
    }

    @Test
    fun `the last evaluated row of the day wins and unknown outcomes stay unlogged`() {
        val rows = listOf(
            DailyComplianceDto(date = "2026-10-07", outcome = "Short", observedEnergyKcal = 900.0),
            DailyComplianceDto(date = "2026-10-07", outcome = "Met", observedEnergyKcal = 1260.0),
        )
        assertEquals(DailyProgress(TODAY, ComplianceOutcome.MET, 1260.0), rows.toDailyProgress(TODAY))

        val unknown = listOf(DailyComplianceDto(date = "2026-10-07", outcome = "Perfect", observedEnergyKcal = 1.0))
        assertEquals(ComplianceOutcome.UNLOGGED, unknown.toDailyProgress(TODAY).outcome)
    }

    @Test
    fun `consistency card invites the patient only with an alert or an unseen prompt`() {
        assertTrue(ConsistencyIndex(ConsistencyState.ALERT, patientPromptPending = false).invitesPatientToReview)
        assertTrue(ConsistencyIndex(ConsistencyState.NORMAL, patientPromptPending = true).invitesPatientToReview)
        assertFalse(ConsistencyIndex(ConsistencyState.WATCH, patientPromptPending = false).invitesPatientToReview)
        assertEquals(ConsistencyState.NORMAL, ConsistencyState.fromCode("Whatever"))
    }

    @Test
    fun `logging gap needs three days without entries and three days of tracking`() {
        assertEquals(TODAY.minusDays(2), LoggingGap.lookbackStart(TODAY))
        assertTrue(LoggingGap.isGap(loggedDaysInLookback = 0, trackingSince = TODAY.minusDays(3), today = TODAY))
        assertFalse(LoggingGap.isGap(loggedDaysInLookback = 1, trackingSince = TODAY.minusDays(30), today = TODAY))
        // Recién vinculado: todavía no hay hueco que señalar.
        assertFalse(LoggingGap.isGap(loggedDaysInLookback = 0, trackingSince = TODAY.minusDays(2), today = TODAY))
    }

    @Test
    fun `gap detection asks for the last three days including today`() = runTest {
        repository.summaryResult = Result.success(summary(loggedDays = 0))

        val gap = DetectLoggingGapUseCase(repository, fixedClock)(12, trackingSince = TODAY.minusDays(10)).getOrThrow()

        assertTrue(gap)
        assertEquals(listOf(TODAY.minusDays(2) to TODAY), repository.summaryRanges)
    }

    @Test
    fun `gap detection does not call the backend for a patient tracked for less than three days`() = runTest {
        val gap = DetectLoggingGapUseCase(repository, fixedClock)(12, trackingSince = TODAY.minusDays(1)).getOrThrow()

        assertFalse(gap)
        assertTrue(repository.summaryRanges.isEmpty())
    }

    @Test
    fun `acknowledging a prompt that was not issued is not an error for the patient`() = runTest {
        repository.acknowledgeResult = failureOf(DomainError.Conflict("ConsistencyPromptNotIssued"))
        assertTrue(AcknowledgeConsistencyPromptUseCase(repository)(12).isSuccess)

        repository.acknowledgeResult = failureOf(DomainError.Unexpected("HTTP_500"))
        assertEquals(DomainError.Unexpected("HTTP_500"), AcknowledgeConsistencyPromptUseCase(repository)(12).domainErrorOrNull())
    }
}
