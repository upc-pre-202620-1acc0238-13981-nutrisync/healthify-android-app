package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/** PT2.1 · El vínculo que espera el consentimiento (el que PT1 recordó al canjear), o `null`. */
class GetPendingCareLinkUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): CareLinkId? =
        careLinkRepository.pendingCareLinkId(PatientId(patientUserId))
}
