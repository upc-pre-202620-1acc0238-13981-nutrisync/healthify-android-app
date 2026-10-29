package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** Home prompt and its screen: the consistency index, or `null` if there is none yet (needs weight and logs). */
class GetConsistencyIndexUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ConsistencyIndex?> =
        repository.getConsistencyIndex(PatientId(patientUserId))
}
