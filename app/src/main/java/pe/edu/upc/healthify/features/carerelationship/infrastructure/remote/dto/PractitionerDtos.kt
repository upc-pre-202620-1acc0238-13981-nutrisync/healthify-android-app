package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `PatientRosterItemResource`. */
@Serializable
data class PatientRosterItemDto(
    val patientId: Long,
    val careLinkId: Long,
    val fullName: String = "",
    val linkedSince: String,
    val linkStatus: String? = null,
    val hasBaseline: Boolean = false,
    val activePlanVersion: Int? = null,
    val isNew: Boolean = false,
    val hasConsultationInProgress: Boolean = false,
    val nextFollowUpAt: String? = null,
    val hasOpenReviewItem: Boolean = false,
)

/** `IssueInvitationResource(ExpiresAt)`. */
@Serializable
data class IssueInvitationRequestDto(val expiresAt: String)

/** `InvitationResource`: `token` only comes in the `POST` response. */
@Serializable
data class InvitationDto(
    val invitationId: Long,
    val token: String? = null,
    val expiresAt: String,
)

/** `DischargePatientResource`. */
@Serializable
data class DischargePatientRequestDto(val clinicalReason: String)
