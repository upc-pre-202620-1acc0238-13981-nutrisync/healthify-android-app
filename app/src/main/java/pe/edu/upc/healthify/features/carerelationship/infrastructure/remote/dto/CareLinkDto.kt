package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `CareLinkResource` (§5.2). Solo los campos que la app usa hoy; el resto se ignora (`ignoreUnknownKeys`).
 * `isActive` exige consentimiento concedido, sin revocación ni alta. `aiProcessingGranted` llega desde CR-2.
 */
@Serializable
data class CareLinkDto(
    val careLinkId: Long,
    val patientId: Long,
    val practitionerId: Long,
    val isActive: Boolean,
    val hasConsent: Boolean = false,
    val aiProcessingGranted: Boolean = false,
    val revokedAt: String? = null,
    val dischargedAt: String? = null,
)

/** `RedeemInvitationResource(Token, ReplaceActiveLink)` (F5, CR-1). */
@Serializable
data class ChangeAiProcessingConsentRequestDto(val granted: Boolean)

@Serializable
data class RedeemInvitationRequestDto(
    val token: String,
    val replaceActiveLink: Boolean,
) {
    override fun toString(): String = "RedeemInvitationRequestDto(token=***, replaceActiveLink=$replaceActiveLink)"
}

/** `GrantConsentResource(Scope, AiProcessingGranted)` (F6, CR-2). */
@Serializable
data class GrantConsentRequestDto(
    val scope: String,
    val aiProcessingGranted: Boolean,
)
