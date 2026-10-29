package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** How **today** is going (the day on the patient's phone). */
class GetTodayProgressUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(patientUserId: Long): Result<DailyProgress> =
        repository.getDailyProgress(PatientId(patientUserId), LocalDate.now(clock))
}
