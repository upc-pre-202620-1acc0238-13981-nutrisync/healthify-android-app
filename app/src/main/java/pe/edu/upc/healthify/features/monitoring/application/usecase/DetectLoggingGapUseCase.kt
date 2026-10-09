package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.LoggingGap
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** PT18: si hay un hueco de registro que merezca una invitación (ver [LoggingGap]). */
class DetectLoggingGapUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
    private val clock: Clock,
) {
    /** @param trackingSince desde cuándo rigen las metas vigentes (inicio aproximado del seguimiento). */
    suspend operator fun invoke(patientUserId: Long, trackingSince: LocalDate): Result<Boolean> {
        val today = LocalDate.now(clock)
        // Sin el tiempo mínimo de seguimiento no hace falta preguntar al backend.
        if (!LoggingGap.isGap(loggedDaysInLookback = 0, trackingSince = trackingSince, today = today)) {
            return Result.success(false)
        }
        return repository.getComplianceSummary(PatientId(patientUserId), LoggingGap.lookbackStart(today), today)
            .map { LoggingGap.isGap(it.loggedDays, trackingSince, today) }
    }
}
