package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.LoggedWeek
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Home "Your week": which of the last 7 days (up to today on the phone) have logs. */
class GetLoggedWeekUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(patientUserId: Long): Result<LoggedWeek> {
        val today = LocalDate.now(clock)
        val from = today.minusDays((LoggedWeek.DAYS - 1).toLong())
        return repository.getComplianceDays(PatientId(patientUserId), from, today)
            .map { LoggedWeek.of(today, it) }
    }
}
