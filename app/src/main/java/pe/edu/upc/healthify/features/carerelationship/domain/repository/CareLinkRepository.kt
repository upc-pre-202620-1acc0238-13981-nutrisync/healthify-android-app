package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus

/** The signed-in patient's care link. */
interface CareLinkRepository {

    /**
     * The patient's care link status. Offline it returns the last state known on the device; if it was never
     * known, it fails with `DomainError.Network`.
     */
    suspend fun getPatientLinkStatus(patientId: PatientId): Result<PatientLinkStatus>

    /**
     * Remembers the care link created when redeeming an invitation while it waits for consent: the backend
     * has no per-patient read of the pending care link (`/care-links/active` only returns consented care
     * links), so it is read later by id with `GET /care-links/{careLinkId}`.
     */
    suspend fun rememberPendingCareLink(patientId: PatientId, careLinkId: CareLinkId)

    /** The pending care link this device remembers, or `null`. */
    suspend fun pendingCareLinkId(patientId: PatientId): CareLinkId?

    /**
     * `POST /invitations/redemption`. [replaceActiveLink] revokes the open care link in the same commit;
     * it is only sent as `true` after the patient confirms switching nutritionists.
     */
    suspend fun redeemInvitation(token: InvitationToken, replaceActiveLink: Boolean): Result<CareLink>

    /** `POST /care-links/{careLinkId}/consent`. Returns the now active care link. */
    suspend fun grantConsent(
        patientId: PatientId,
        careLinkId: CareLinkId,
        scope: ConsentScope,
        aiProcessingGranted: Boolean,
    ): Result<CareLink>

    /** `GET /patients/{patientId}/care-links/active`: the consented care link (`404 CareLinkNotFound` if none). */
    suspend fun getActiveCareLink(patientId: PatientId): Result<CareLink>

    /** `DELETE /care-links/{careLinkId}/consent`: revokes the care link and turns AI off. */
    suspend fun withdrawConsent(patientId: PatientId, careLinkId: CareLinkId): Result<Unit>

    /** `PUT /care-links/{careLinkId}/ai-processing-consent`: the separate AI consent. */
    suspend fun changeAiProcessingConsent(careLinkId: CareLinkId, granted: Boolean): Result<Unit>

    /** Saves on the device that the patient no longer has a care link (withdrawal without current consent). */
    suspend fun markLinkClosed(patientId: PatientId)

    /** `GET /care-links/{careLinkId}/targets-read-status`. */
    suspend fun getTargetsReadStatus(careLinkId: CareLinkId): Result<TargetsReadStatus>

    /** `POST /care-links/{careLinkId}/targets-acknowledgement`: "I've reviewed them". */
    suspend fun acknowledgeActiveTargets(careLinkId: CareLinkId, planVersion: Int): Result<Unit>
}
