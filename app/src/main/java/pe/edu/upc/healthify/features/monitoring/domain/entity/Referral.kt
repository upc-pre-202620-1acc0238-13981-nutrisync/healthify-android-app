package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** Referral specialty (`Specialty`, 1–120 characters). */
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

/** Clinical reason for a referral (`Reason`, 1–1000 characters). The practitioner's text: never translated. */
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

/** A new referral (`RecordReferralResource`). Records that something was decided on a date; it has no workflow. */
data class NewReferral(val patientId: PatientId, val specialty: ReferralSpecialty, val reason: ReferralReason)

/** Missing field of the referral form. */
enum class ReferralField { SPECIALTY, REASON }

/** Referral form validation before sending (`400 SpecialtyAndReasonRequired` in the backend). */
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
