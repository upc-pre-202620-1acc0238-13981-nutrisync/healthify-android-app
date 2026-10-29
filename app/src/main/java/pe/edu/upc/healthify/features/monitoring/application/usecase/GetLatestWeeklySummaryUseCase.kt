package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.WeeklySummaryRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** "Your week" card and screen: the latest weekly summary, "not yet" or off. */
class GetLatestWeeklySummaryUseCase @Inject constructor(
    private val repository: WeeklySummaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<WeeklySummaryAvailability> =
        repository.getLatest(PatientId(patientUserId))
}
