package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT1 · Canjear la invitación leída del QR (F5). El vínculo nace sin consentimiento: se recuerda en el dispositivo
 * para que S5 lleve a PT2.1 si el paciente sale antes de consentir.
 *
 * @param replaceActiveLink `true` **solo** cuando el paciente confirmó PT21.V (CR-1).
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
        /** Mismo código que el backend (F5 caso 1): la UI muestra un solo mensaje para ambos orígenes. */
        const val CODE_INVITATION_NOT_VALID = "InvitationNotValid"
    }
}
