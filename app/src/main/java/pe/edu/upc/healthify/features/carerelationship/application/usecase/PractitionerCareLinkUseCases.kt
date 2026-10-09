package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.repository.PractitionerCareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import java.time.Clock
import javax.inject.Inject

/** PR1 · la cartera del nutricionista con sesión. */
class GetPatientRosterUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
) {
    suspend operator fun invoke(practitionerUserId: Long): Result<PatientRoster> = repository.getRoster(practitionerUserId)
}

/** PR2 · emite una invitación que vence en [validity] desde ahora («Generar otro código» emite una nueva). */
class IssueInvitationUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(validity: InvitationValidity): Result<IssuedInvitation> =
        repository.issueInvitation(validity.expiresAt(clock.instant()))
}

/** PAC-4 · si el paciente vio las metas publicadas («Ana las vio el mismo día»), leído por el vínculo (CR-3). */
class GetLinkTargetsReadStatusUseCase @Inject constructor(
    private val repository: CareLinkRepository,
) {
    suspend operator fun invoke(careLinkId: Long): Result<TargetsReadStatus> =
        repository.getTargetsReadStatus(CareLinkId(careLinkId))
}

/** PR18 «Sí, dar de alta»: el motivo clínico es obligatorio (la regla vive en [ClinicalReason]). */
class DischargePatientUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
) {
    suspend operator fun invoke(careLinkId: Long, reason: ClinicalReason): Result<Unit> =
        repository.discharge(CareLinkId(careLinkId), reason)
}
