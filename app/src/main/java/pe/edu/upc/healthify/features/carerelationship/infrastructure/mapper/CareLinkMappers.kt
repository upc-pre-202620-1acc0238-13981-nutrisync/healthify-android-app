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
    // Un vínculo activo siempre tiene consentimiento, aunque la respuesta no traiga el campo.
    hasConsent = hasConsent || isActive,
    aiProcessingGranted = aiProcessingGranted,
    isRevoked = revokedAt != null,
    isDischarged = dischargedAt != null,
)

/** Activo → [PatientLinkStatus.ACTIVE]; abierto sin consentimiento → pendiente; revocado o dado de alta → sin vínculo. */
fun CareLinkDto.toPatientLinkStatus(): PatientLinkStatus = toDomain().patientStatus

fun InvitationToken.toRedeemDto(replaceActiveLink: Boolean) =
    RedeemInvitationRequestDto(token = value, replaceActiveLink = replaceActiveLink)

fun ConsentScope.toGrantDto(aiProcessingGranted: Boolean) =
    GrantConsentRequestDto(scope = value, aiProcessingGranted = aiProcessingGranted)

fun TargetsReadStatusDto.toDomain(): TargetsReadStatus = TargetsReadStatus(
    careLinkId = CareLinkId(careLinkId),
    // Una versión no positiva no es una versión: se lee como «nada pendiente».
    pendingVersion = pendingTargetsVersion?.takeIf { it > 0 },
    lastAcknowledgedVersion = lastAcknowledgedVersion?.takeIf { it > 0 },
    hasPendingAcknowledgement = hasPendingAcknowledgement,
    lastAcknowledgedAt = lastAcknowledgedAt?.toInstantOrNull(),
)
