package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import java.time.Instant

/**
 * Lado del nutricionista de CareRelationship. Nada se guarda en el teléfono: la cartera no está disponible sin
 * conexión (PR1.O) y el token de una invitación se muestra una sola vez.
 */
interface PractitionerCareLinkRepository {

    /** `GET /practitioners/{uid}/patient-roster` (RM-1). `403 AccessNotAllowed` si no es la cartera propia. */
    suspend fun getRoster(practitionerId: Long): Result<PatientRoster>

    /** `POST /invitations` (F4). `400 ExpirationDateRequired` si [expiresAt] no es futura. */
    suspend fun issueInvitation(expiresAt: Instant): Result<IssuedInvitation>

    /**
     * `POST /care-links/{id}/discharge` (F29, CR-4). `400 ClinicalReasonRequired`, `403 PractitionerOnly`,
     * `404 CareLinkNotFound` y `409 DischargedLinkCannotBeReactivated` (ya estaba dado de alta).
     */
    suspend fun discharge(careLinkId: CareLinkId, reason: ClinicalReason): Result<Unit>
}
