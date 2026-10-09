package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import javax.inject.Inject

/** S5: si el paciente no tiene vínculo (PT1), lo tiene pendiente de consentimiento (PT2.1) o activo (PT3). */
class GetPatientLinkStatusUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<PatientLinkStatus> =
        careLinkRepository.getPatientLinkStatus(PatientId(patientUserId))
}
