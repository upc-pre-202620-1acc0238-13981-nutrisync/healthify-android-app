package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** Especialidad de una derivación (`Specialty`, 1–120 caracteres, F25). */
@JvmInline
value class ReferralSpecialty(val text: String) {
    init {
        require(text.isNotBlank()) { "A referral needs a specialty" }
        require(text.trim().length <= MAX_LENGTH) { "A specialty has at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 120
    }
}

/** Motivo clínico de una derivación (`Reason`, 1–1000 caracteres, F25). Texto del profesional: nunca se traduce. */
@JvmInline
value class ReferralReason(val text: String) {
    init {
        require(text.isNotBlank()) { "A referral needs a reason" }
        require(text.trim().length <= MAX_LENGTH) { "A reason has at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

/** Una derivación nueva (`RecordReferralResource`). Registra que algo se decidió en una fecha; no tiene flujo. */
data class NewReferral(val patientId: PatientId, val specialty: ReferralSpecialty, val reason: ReferralReason)

/** Campo de PR16 que falta (PR16.E). */
enum class ReferralField { SPECIALTY, REASON }

/** Validación de PR16 antes de enviar (`400 SpecialtyAndReasonRequired` en el backend). */
sealed interface ReferralValidation {
    data class Valid(val referral: NewReferral) : ReferralValidation
    data class Invalid(val missing: Set<ReferralField>) : ReferralValidation

    companion object {
        fun of(patientId: PatientId, specialty: String, reason: String): ReferralValidation {
            val missing = buildSet {
                if (specialty.isBlank()) add(ReferralField.SPECIALTY)
                if (reason.isBlank()) add(ReferralField.REASON)
            }
            return if (missing.isEmpty()) {
                Valid(NewReferral(patientId, ReferralSpecialty(specialty.trim()), ReferralReason(reason.trim())))
            } else {
                Invalid(missing)
            }
        }
    }
}
