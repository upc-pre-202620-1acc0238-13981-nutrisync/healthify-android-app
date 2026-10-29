package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * Redeems the invitation read from the QR code. The care link starts without consent: the device remembers it
 * so the app returns to the pending consent screen if the patient leaves before consenting.
 *
 * @param replaceActiveLink `true` **only** when the patient confirmed switching nutritionists.
 */
class RedeemInvitationUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        scannedContent: String,
        replaceActiveLink: Boolean,
    ): Result<CareLink> {
        val token = InvitationToken.parse(scannedContent)
            ?: return domainFailure(DomainError.Validation(CODE_INVITATION_NOT_VALID))
        val patientId = PatientId(patientUserId)
        return careLinkRepository.redeemInvitation(token, replaceActiveLink).onSuccess { link ->
            careLinkRepository.rememberPendingCareLink(patientId, link.id)
        }
    }

    companion object {
        /** Same code as the backend: the UI shows a single message for both sources. */
        const val CODE_INVITATION_NOT_VALID = "InvitationNotValid"
    }
}
