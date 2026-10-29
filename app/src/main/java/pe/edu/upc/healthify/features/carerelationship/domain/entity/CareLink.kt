package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus

/**
 * Care link between a patient and their nutritionist (`CareLinkResource`). It starts inactive when the invitation
 * is redeemed and only consent activates it. Once revoked or discharged, it stays closed for good.
 *
 * @param aiProcessingGranted the patient accepted the AI features; decided separately from consent.
 */
data class CareLink(
    val id: CareLinkId,
    val patientId: PatientId,
    val isActive: Boolean,
    val hasConsent: Boolean,
    val aiProcessingGranted: Boolean,
    val isRevoked: Boolean,
    val isDischarged: Boolean,
) {
    init {
        require(!isActive || (hasConsent && !isRevoked && !isDischarged)) {
            "An active CareLink needs consent and must not be closed"
        }
    }

    val isClosed: Boolean get() = isRevoked || isDischarged

    /** Where this link takes the patient: active → home, unconsented → pending consent, closed → scan invitation. */
    val patientStatus: PatientLinkStatus
        get() = when {
            isActive -> PatientLinkStatus.ACTIVE
            !isClosed && !hasConsent -> PatientLinkStatus.PENDING_CONSENT
            else -> PatientLinkStatus.NO_LINK
        }
}
