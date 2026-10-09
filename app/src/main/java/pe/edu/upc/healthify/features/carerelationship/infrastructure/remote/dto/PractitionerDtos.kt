package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `PatientRosterItemResource` (RM-1). */
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
)

/** `IssueInvitationResource(ExpiresAt)` (F4). */
@Serializable
data class IssueInvitationRequestDto(val expiresAt: String)

/** `InvitationResource` (F4): `token` solo viene en la respuesta del `POST`. */
@Serializable
data class InvitationDto(
    val invitationId: Long,
    val token: String? = null,
    val expiresAt: String,
)

/** `DischargePatientResource` (F29). */
@Serializable
data class DischargePatientRequestDto(val clinicalReason: String)
