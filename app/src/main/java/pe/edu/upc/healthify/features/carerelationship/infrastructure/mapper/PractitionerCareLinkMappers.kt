package pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper

import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.entity.RosterPatient
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.InvitationDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.PatientRosterItemDto

/** A patient without a readable name or link date is skipped: the rest of the roster still shows. */
fun List<PatientRosterItemDto>.toRoster(): PatientRoster = PatientRoster(
    mapNotNull { dto ->
        val since = dto.linkedSince.toInstantOrNull() ?: return@mapNotNull null
        if (dto.fullName.isBlank() || dto.patientId <= 0 || dto.careLinkId <= 0) return@mapNotNull null
        RosterPatient(
            patientId = dto.patientId,
            careLinkId = CareLinkId(dto.careLinkId),
            fullName = dto.fullName.trim(),
            linkedSince = since,
            isLinkActive = dto.linkStatus.equals("Active", ignoreCase = true),
            hasBaseline = dto.hasBaseline,
            activePlanVersion = dto.activePlanVersion,
            isNew = dto.isNew,
            hasConsultationInProgress = dto.hasConsultationInProgress,
            nextFollowUpAt = dto.nextFollowUpAt?.toInstantOrNull(),
            hasOpenReviewItem = dto.hasOpenReviewItem,
        )
    },
)

/** Without a token shaped like an invitation, the response is useless for the QR code. */
fun InvitationDto.toDomain(): IssuedInvitation = IssuedInvitation(
    invitationId = invitationId,
    token = token?.let(InvitationToken::parse) ?: throw SerializationException("Invitation without a usable token"),
    expiresAt = expiresAt.toInstantOrNull() ?: throw SerializationException("Unreadable expiresAt"),
)
