package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** Home "Next visit" row: `null` if none is scheduled (the row is hidden). */
class GetNextFollowUpUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<NextFollowUp?> =
        repository.getNextFollowUp(PatientId(patientUserId))
}
