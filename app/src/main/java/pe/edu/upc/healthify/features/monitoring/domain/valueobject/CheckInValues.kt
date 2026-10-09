package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/** «¿Cómo te sentiste con tu plan?» (`PlanFeeling`, MA-4). */
enum class PlanFeeling(val code: String) {
    GOOD("Good"),
    FAIR("Fair"),
    HARD("Hard"),
    ;

    companion object {
        fun fromCode(code: String?): PlanFeeling? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/** «¿Qué te costó más?» (`CheckInDifficulty`, MA-4): lista cerrada, se pueden elegir varias. */
enum class CheckInDifficulty(val code: String) {
    DINNERS("Dinners"),
    WEEKENDS("Weekends"),
    EATING_OUT("EatingOut"),
    SCHEDULES("Schedules"),
    CRAVINGS("Cravings"),
    ;

    companion object {
        fun fromCode(code: String?): CheckInDifficulty? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/** De quién es una pregunta del check-in (`QuestionOrigin`): escrita por el paciente o una sugerencia de IA aceptada. */
enum class QuestionOrigin(val code: String) {
    PATIENT("Patient"),
    AI_SUGGESTED("AiSuggested"),
    ;

    companion object {
        /** Sin valor = del paciente (como el backend). */
        fun fromCode(code: String?): QuestionOrigin =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: PATIENT
    }
}

/**
 * Una pregunta para la consulta (`PatientQuestion`, MA-4): 3 a 300 caracteres sin espacios sobrantes. La del paciente
 * se guarda y se muestra **tal cual** (nunca se traduce); la de IA lleva el idioma en que se generó (X-2, `es`/`en`) y
 * la generación de la que salió.
 */
data class CheckInQuestion(
    val text: String,
    val origin: QuestionOrigin,
    val aiGenerationId: Long? = null,
    val language: String? = null,
) {
    init {
        require(text == text.trim()) { "A question is trimmed" }
        require(text.length in MIN_LENGTH..MAX_LENGTH) { "A question has between $MIN_LENGTH and $MAX_LENGTH characters" }
        require(origin == QuestionOrigin.AI_SUGGESTED || (aiGenerationId == null && language == null)) {
            "Only an AI suggestion carries a generation and a language"
        }
        require(language == null || language in LANGUAGES) { "A question language is es or en" }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 300
        private val LANGUAGES = setOf("es", "en")

        /** La pregunta libre del paciente: vacía = no hay pregunta; corta o larga = inválida. */
        fun ownQuestion(raw: String): OwnQuestion {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> OwnQuestion.Empty
                trimmed.length < MIN_LENGTH -> OwnQuestion.TooShort
                trimmed.length > MAX_LENGTH -> OwnQuestion.TooLong
                else -> OwnQuestion.Valid(CheckInQuestion(trimmed, QuestionOrigin.PATIENT))
            }
        }

        /** Una sugerencia de IA aceptada, con su idioma (si no es `es`/`en`, se manda sin idioma). */
        fun aiSuggestion(text: String, aiGenerationId: Long?, language: String?): CheckInQuestion? {
            val trimmed = text.trim()
            if (trimmed.length !in MIN_LENGTH..MAX_LENGTH) return null
            val normalized = language?.trim()?.lowercase()?.takeIf { it in LANGUAGES }
            return CheckInQuestion(trimmed, QuestionOrigin.AI_SUGGESTED, aiGenerationId, normalized)
        }
    }
}

sealed interface OwnQuestion {
    data object Empty : OwnQuestion
    data object TooShort : OwnQuestion
    data object TooLong : OwnQuestion
    data class Valid(val question: CheckInQuestion) : OwnQuestion
}
