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

/** The signed-in nutritionist's patient roster. */
class GetPatientRosterUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
) {
    suspend operator fun invoke(practitionerUserId: Long): Result<PatientRoster> = repository.getRoster(practitionerUserId)
}

/** Issues an invitation that expires [validity] from now ("Generate another code" issues a new one). */
class IssueInvitationUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(validity: InvitationValidity): Result<IssuedInvitation> =
        repository.issueInvitation(validity.expiresAt(clock.instant()))
}

/** Whether the patient saw the published targets ("Ana saw them the same day"), read through the care link. */
class GetLinkTargetsReadStatusUseCase @Inject constructor(
    private val repository: CareLinkRepository,
) {
    suspend operator fun invoke(careLinkId: Long): Result<TargetsReadStatus> =
        repository.getTargetsReadStatus(CareLinkId(careLinkId))
}

/** "Yes, discharge": the clinical reason is required (the rule lives in [ClinicalReason]). */
class DischargePatientUseCase @Inject constructor(
    private val repository: PractitionerCareLinkRepository,
) {
    suspend operator fun invoke(careLinkId: Long, reason: ClinicalReason): Result<Unit> =
        repository.discharge(CareLinkId(careLinkId), reason)
}
