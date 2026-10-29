package pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.CareLinkDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.GrantConsentRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.RedeemInvitationRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.TargetsReadStatusDto

fun CareLinkDto.toDomain(): CareLink = CareLink(
    id = CareLinkId(careLinkId),
    patientId = PatientId(patientId),
    isActive = isActive,
    // An active care link always has consent, even if the response omits the field.
    hasConsent = hasConsent || isActive,
    aiProcessingGranted = aiProcessingGranted,
    isRevoked = revokedAt != null,
    isDischarged = dischargedAt != null,
)

/** Active → [PatientLinkStatus.ACTIVE]; open without consent → pending; revoked or discharged → no care link. */
fun CareLinkDto.toPatientLinkStatus(): PatientLinkStatus = toDomain().patientStatus

fun InvitationToken.toRedeemDto(replaceActiveLink: Boolean) =
    RedeemInvitationRequestDto(token = value, replaceActiveLink = replaceActiveLink)

fun ConsentScope.toGrantDto(aiProcessingGranted: Boolean) =
    GrantConsentRequestDto(scope = value, aiProcessingGranted = aiProcessingGranted)

fun TargetsReadStatusDto.toDomain(): TargetsReadStatus = TargetsReadStatus(
    careLinkId = CareLinkId(careLinkId),
    // A non-positive version is not a version: read it as "nothing pending".
    pendingVersion = pendingTargetsVersion?.takeIf { it > 0 },
    lastAcknowledgedVersion = lastAcknowledgedVersion?.takeIf { it > 0 },
    hasPendingAcknowledgement = hasPendingAcknowledgement,
    lastAcknowledgedAt = lastAcknowledgedAt?.toInstantOrNull(),
)
