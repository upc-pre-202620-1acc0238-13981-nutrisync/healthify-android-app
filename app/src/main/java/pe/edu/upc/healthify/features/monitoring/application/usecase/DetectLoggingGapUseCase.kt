package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.LoggingGap
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Whether there is a logging gap worth an invitation (see [LoggingGap]). */
class DetectLoggingGapUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
    private val clock: Clock,
) {
    /** @param trackingSince since when the current goals apply (approximate start of tracking). */
    suspend operator fun invoke(patientUserId: Long, trackingSince: LocalDate): Result<Boolean> {
        val today = LocalDate.now(clock)
        // Without the minimum tracking time there is no need to ask the backend.
        if (!LoggingGap.isGap(loggedDaysInLookback = 0, trackingSince = trackingSince, today = today)) {
            return Result.success(false)
        }
        return repository.getComplianceSummary(PatientId(patientUserId), LoggingGap.lookbackStart(today), today)
            .map { LoggingGap.isGap(it.loggedDays, trackingSince, today) }
    }
}
