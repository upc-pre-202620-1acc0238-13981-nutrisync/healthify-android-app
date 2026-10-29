package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.LocalDate

/** Patient-side MonitoringAdherence reads. None of them work offline. */
interface PatientMonitoringRepository {

    /** `GET …/daily-compliance?date=`: an unevaluated day is `Unlogged` with 0 kcal. */
    suspend fun getDailyProgress(patientId: PatientId, date: LocalDate): Result<DailyProgress>

    /** `GET …/daily-compliance?from=&to=` (max. 31 days). */
    suspend fun getComplianceSummary(patientId: PatientId, from: LocalDate, to: LocalDate): Result<ComplianceSummary>

    /** `GET …/daily-compliance?from=&to=`: the outcome of each day in the range that the backend evaluated. */
    suspend fun getComplianceDays(patientId: PatientId, from: LocalDate, to: LocalDate): Result<List<ComplianceDay>>

    /** `GET …/consistency-index`; `null` if there is no index yet (`422 BothSeriesRequired`). */
    suspend fun getConsistencyIndex(patientId: PatientId): Result<ConsistencyIndex?>

    /** `POST …/consistency-index/prompt-acknowledgement`: the patient saw the prompt. */
    suspend fun acknowledgeConsistencyPrompt(patientId: PatientId): Result<Unit>

    /** `GET …/scheduled-follow-ups/next`; `null` if none is scheduled (`404`). */
    suspend fun getNextFollowUp(patientId: PatientId): Result<NextFollowUp?>
}
