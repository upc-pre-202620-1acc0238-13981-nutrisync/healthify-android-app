package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * En qué punto está el paciente con su nutricionista. Decide adónde va el paciente al entrar (S5):
 * registrarse no da acceso a nada (F1); el vínculo nace inactivo al canjear la invitación (F5) y solo el
 * consentimiento lo activa (F6).
 */
enum class PatientLinkStatus {
    /** Sin vínculo abierto: escanear la invitación (PT1). */
    NO_LINK,

    /** Invitación canjeada, falta el consentimiento (PT2.1). */
    PENDING_CONSENT,

    /** Vínculo activo: Inicio (PT3). */
    ACTIVE,
}
