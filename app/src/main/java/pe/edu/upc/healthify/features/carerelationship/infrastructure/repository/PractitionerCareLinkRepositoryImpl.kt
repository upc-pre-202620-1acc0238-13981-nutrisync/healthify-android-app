package pe.edu.upc.healthify.features.carerelationship.infrastructure.repository

import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.repository.PractitionerCareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toRoster
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.PractitionerCareLinkService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.DischargePatientRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.IssueInvitationRequestDto
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Los mappers van dentro de `apiCall`: una respuesta ilegible se lee como `Unexpected("MALFORMED_RESPONSE")`. */
class PractitionerCareLinkRepositoryImpl @Inject constructor(
    private val service: PractitionerCareLinkService,
) : PractitionerCareLinkRepository {

    override suspend fun getRoster(practitionerId: Long): Result<PatientRoster> =
        apiCall { service.getRoster(practitionerId).toRoster() }

    override suspend fun issueInvitation(expiresAt: Instant): Result<IssuedInvitation> =
        apiCall {
            service.issueInvitation(IssueInvitationRequestDto(expiresAt.truncatedTo(ChronoUnit.SECONDS).toString()))
                .toDomain()
        }

    override suspend fun discharge(careLinkId: CareLinkId, reason: ClinicalReason): Result<Unit> =
        apiCall { service.discharge(careLinkId.value, DischargePatientRequestDto(reason.text)) }.map { }
}
