package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/** The care link waiting for consent (the one remembered when the invitation was redeemed), or `null`. */
class GetPendingCareLinkUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): CareLinkId? =
        careLinkRepository.pendingCareLinkId(PatientId(patientUserId))
}
