package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/** Visit modality (`ConsultationModality`). Missing or unknown = in person (the backend default). */
enum class ConsultationModality(val code: String) {
    IN_PERSON("InPerson"),
    REMOTE("Remote"),
    ;

    companion object {
        fun fromCode(code: String?): ConsultationModality =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: IN_PERSON
    }
}

/**
 * A visit preparation instruction (`PreparationInstruction`): a catalog code, which the app translates, or a code
 * this version does not know, shown as is (unknown -> `Custom`).
 */
sealed interface PreparationInstruction {

    data class Catalog(val code: PreparationCode) : PreparationInstruction

    data class Custom(val text: String) : PreparationInstruction {
        init {
            require(text.isNotBlank()) { "A custom preparation needs text" }
        }
    }

    companion object {
        /** `null` if empty. */
        fun of(code: String?): PreparationInstruction? {
            val known = PreparationCode.fromCode(code)
            return when {
                known != null -> Catalog(known)
                !code.isNullOrBlank() -> Custom(code.trim())
                else -> null
            }
        }
    }
}

enum class PreparationCode(val code: String) {
    FASTING("Fasting"),
    LIGHT_CLOTHING("LightClothing"),
    BRING_BLOOD_TESTS("BringBloodTests"),
    EMPTY_BLADDER("EmptyBladder"),
    ;

    companion object {
        fun fromCode(code: String?): PreparationCode? = entries.firstOrNull { it.code == code?.trim() }
    }
}

/** What a past visit was (`PastConsultationResource.label`). */
sealed interface ConsultationLabel {
    data object FirstConsultation : ConsultationLabel
    data object AssessmentAndNewPlan : ConsultationLabel

    /** A code this version does not know: shown as is. */
    data class Custom(val text: String) : ConsultationLabel {
        init {
            require(text.isNotBlank()) { "A custom label needs text" }
        }
    }

    companion object {
        /** `null` if empty (the row shows only the date). */
        fun of(code: String?): ConsultationLabel? = when (val trimmed = code?.trim()) {
            null, "" -> null
            "FirstConsultation" -> FirstConsultation
            "AssessmentAndNewPlan" -> AssessmentAndNewPlan
            else -> Custom(trimmed)
        }
    }
}
