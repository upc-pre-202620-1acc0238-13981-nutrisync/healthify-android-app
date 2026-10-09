package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.LocalDate

/** Lecturas de MonitoringAdherence del lado del paciente (§5.6). Ninguna funciona sin conexión. */
interface PatientMonitoringRepository {

    /** `GET …/daily-compliance?date=`: un día sin evaluar es `Unlogged` con 0 kcal. */
    suspend fun getDailyProgress(patientId: PatientId, date: LocalDate): Result<DailyProgress>

    /** `GET …/daily-compliance?from=&to=` (MA-6, máx. 31 días). */
    suspend fun getComplianceSummary(patientId: PatientId, from: LocalDate, to: LocalDate): Result<ComplianceSummary>

    /** `GET …/consistency-index`; `null` si todavía no hay índice (`422 BothSeriesRequired`). */
    suspend fun getConsistencyIndex(patientId: PatientId): Result<ConsistencyIndex?>

    /** `POST …/consistency-index/prompt-acknowledgement` (MA-7): el paciente vio el aviso. */
    suspend fun acknowledgeConsistencyPrompt(patientId: PatientId): Result<Unit>

    /** `GET …/scheduled-follow-ups/next` (MA-3); `null` si no hay ninguna agendada (`404`). */
    suspend fun getNextFollowUp(patientId: PatientId): Result<NextFollowUp?>
}
