package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus

/** Vínculo asistencial del paciente con sesión (§5.2 CareRelationship). */
interface CareLinkRepository {

    /**
     * Estado del vínculo del paciente. Sin conexión devuelve el último estado conocido en el dispositivo; si nunca
     * se supo, falla con `DomainError.Network`.
     */
    suspend fun getPatientLinkStatus(patientId: PatientId): Result<PatientLinkStatus>

    /**
     * Recuerda el vínculo recién creado al canjear una invitación (PT1) mientras espera el consentimiento: el backend
     * no tiene una lectura del vínculo pendiente por paciente (`/care-links/active` solo devuelve vínculos con
     * consentimiento), así que se consulta luego por su id con `GET /care-links/{careLinkId}`.
     */
    suspend fun rememberPendingCareLink(patientId: PatientId, careLinkId: CareLinkId)

    /** El vínculo pendiente que este dispositivo recuerda (PT2.1 → PT2), o `null`. */
    suspend fun pendingCareLinkId(patientId: PatientId): CareLinkId?

    /**
     * `POST /invitations/redemption` (F5). [replaceActiveLink] revoca el vínculo sin cerrar en el mismo commit
     * (CR-1); solo se manda en `true` tras la confirmación de PT21.V.
     */
    suspend fun redeemInvitation(token: InvitationToken, replaceActiveLink: Boolean): Result<CareLink>

    /** `POST /care-links/{careLinkId}/consent` (F6, CR-2). Devuelve el vínculo ya activo. */
    suspend fun grantConsent(
        patientId: PatientId,
        careLinkId: CareLinkId,
        scope: ConsentScope,
        aiProcessingGranted: Boolean,
    ): Result<CareLink>

    /** `GET /patients/{patientId}/care-links/active`: el vínculo con consentimiento (`404 CareLinkNotFound` si no hay). */
    suspend fun getActiveCareLink(patientId: PatientId): Result<CareLink>

    /** `DELETE /care-links/{careLinkId}/consent` (F28): revoca el vínculo y apaga la IA. */
    suspend fun withdrawConsent(patientId: PatientId, careLinkId: CareLinkId): Result<Unit>

    /** `PUT /care-links/{careLinkId}/ai-processing-consent` (CR-2, PT21.IA): el consentimiento aparte de la IA. */
    suspend fun changeAiProcessingConsent(careLinkId: CareLinkId, granted: Boolean): Result<Unit>

    /** Guarda en el dispositivo que el paciente ya no tiene vínculo (PT23 sin consentimiento vigente). */
    suspend fun markLinkClosed(patientId: PatientId)

    /** `GET /care-links/{careLinkId}/targets-read-status` (F12, CR-3). */
    suspend fun getTargetsReadStatus(careLinkId: CareLinkId): Result<TargetsReadStatus>

    /** `POST /care-links/{careLinkId}/targets-acknowledgement` (F12): «Ya las revisé». */
    suspend fun acknowledgeActiveTargets(careLinkId: CareLinkId, planVersion: Int): Result<Unit>
}
