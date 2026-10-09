package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/** Modalidad de la consulta (`ConsultationModality`, MA-2). Sin valor o desconocida = presencial (default del backend). */
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
 * Una indicación de preparación de la consulta (`PreparationInstruction`, MA-2): un código del catálogo, que la app
 * traduce, o un código que esta versión no conoce, que se muestra tal cual (X-2: desconocido → `Custom`).
 */
sealed interface PreparationInstruction {

    data class Catalog(val code: PreparationCode) : PreparationInstruction

    data class Custom(val text: String) : PreparationInstruction {
        init {
            require(text.isNotBlank()) { "A custom preparation needs text" }
        }
    }

    companion object {
        /** `null` si viene vacío. */
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

/** Qué fue una consulta anterior (`PastConsultationResource.label`, RM-5). */
sealed interface ConsultationLabel {
    data object FirstConsultation : ConsultationLabel
    data object AssessmentAndNewPlan : ConsultationLabel

    /** Un código que esta versión no conoce: se muestra tal cual. */
    data class Custom(val text: String) : ConsultationLabel {
        init {
            require(text.isNotBlank()) { "A custom label needs text" }
        }
    }

    companion object {
        /** `null` si viene vacío (la fila muestra solo la fecha). */
        fun of(code: String?): ConsultationLabel? = when (val trimmed = code?.trim()) {
            null, "" -> null
            "FirstConsultation" -> FirstConsultation
            "AssessmentAndNewPlan" -> AssessmentAndNewPlan
            else -> Custom(trimmed)
        }
    }
}
