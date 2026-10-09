package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus

/**
 * Vínculo asistencial entre un paciente y su nutricionista (`CareLinkResource`). Nace inactivo al canjear la
 * invitación (F5) y solo el consentimiento lo activa (F6). Revocado o dado de alta, queda cerrado para siempre.
 *
 * @param aiProcessingGranted el paciente aceptó las funciones con IA (CR-2); se decide aparte del consentimiento.
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

    /** Adónde lleva este vínculo al paciente: activo → PT3, abierto sin consentimiento → PT2.1, cerrado → PT1. */
    val patientStatus: PatientLinkStatus
        get() = when {
            isActive -> PatientLinkStatus.ACTIVE
            !isClosed && !hasConsent -> PatientLinkStatus.PENDING_CONSENT
            else -> PatientLinkStatus.NO_LINK
        }
}
