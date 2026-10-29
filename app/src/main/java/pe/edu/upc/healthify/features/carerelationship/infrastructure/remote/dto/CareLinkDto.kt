package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `CareLinkResource`. Only the fields the app uses today; the rest is ignored (`ignoreUnknownKeys`).
 * `isActive` requires granted consent, with no revocation or discharge. Older responses may omit `aiProcessingGranted`.
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

/** `RedeemInvitationResource(Token, ReplaceActiveLink)`. */
@Serializable
data class ChangeAiProcessingConsentRequestDto(val granted: Boolean)

@Serializable
data class RedeemInvitationRequestDto(
    val token: String,
    val replaceActiveLink: Boolean,
) {
    override fun toString(): String = "RedeemInvitationRequestDto(token=***, replaceActiveLink=$replaceActiveLink)"
}

/** `GrantConsentResource(Scope, AiProcessingGranted)`. */
@Serializable
data class GrantConsentRequestDto(
    val scope: String,
    val aiProcessingGranted: Boolean,
)
