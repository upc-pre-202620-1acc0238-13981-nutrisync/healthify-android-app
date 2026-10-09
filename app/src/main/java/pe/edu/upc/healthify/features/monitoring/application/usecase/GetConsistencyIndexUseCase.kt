package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** PT3 aviso / PT16: el índice de consistencia, o `null` si todavía no hay (hacen falta peso y registros). */
class GetConsistencyIndexUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ConsistencyIndex?> =
        repository.getConsistencyIndex(PatientId(patientUserId))
}
