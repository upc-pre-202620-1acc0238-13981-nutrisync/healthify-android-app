package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.WeeklySummaryRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** PT13 card «Tu semana» y PT13.2: el último resumen semanal, «aún no» o apagado. */
class GetLatestWeeklySummaryUseCase @Inject constructor(
    private val repository: WeeklySummaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<WeeklySummaryAvailability> =
        repository.getLatest(PatientId(patientUserId))
}
