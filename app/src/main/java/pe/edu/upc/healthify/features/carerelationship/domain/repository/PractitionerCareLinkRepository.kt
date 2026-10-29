package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import java.time.Instant

/**
 * The nutritionist side of CareRelationship. Nothing is stored on the phone: the roster is not available
 * offline and an invitation token is shown only once.
 */
interface PractitionerCareLinkRepository {

    /** `GET /practitioners/{uid}/patient-roster`. `403 AccessNotAllowed` if it is not the caller's own roster. */
    suspend fun getRoster(practitionerId: Long): Result<PatientRoster>

    /** `POST /invitations`. `400 ExpirationDateRequired` if [expiresAt] is not in the future. */
    suspend fun issueInvitation(expiresAt: Instant): Result<IssuedInvitation>

    /**
     * `POST /care-links/{id}/discharge`. `400 ClinicalReasonRequired`, `403 PractitionerOnly`,
     * `404 CareLinkNotFound` and `409 DischargedLinkCannotBeReactivated` (already discharged).
     */
    suspend fun discharge(careLinkId: CareLinkId, reason: ClinicalReason): Result<Unit>
}
