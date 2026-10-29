package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import javax.inject.Inject

/** Whether the patient has no care link, one waiting for consent, or an active one. */
class GetPatientLinkStatusUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<PatientLinkStatus> =
        careLinkRepository.getPatientLinkStatus(PatientId(patientUserId))
}
