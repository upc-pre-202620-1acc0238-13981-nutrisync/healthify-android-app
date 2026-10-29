package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/** "How did you feel about your plan?" (`PlanFeeling`). */
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

/** "What was hardest?" (`CheckInDifficulty`): a closed list, several can be picked. */
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

/** Who a check-in question comes from (`QuestionOrigin`): written by the patient or an accepted AI suggestion. */
enum class QuestionOrigin(val code: String) {
    PATIENT("Patient"),
    AI_SUGGESTED("AiSuggested"),
    ;

    companion object {
        /** No value = the patient's (as in the backend). */
        fun fromCode(code: String?): QuestionOrigin =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: PATIENT
    }
}

/**
 * A question for the visit (`PatientQuestion`): 3 to 300 characters without extra spaces. The patient's is saved
 * and shown **as is** (never translated); the AI one carries the language it was generated in (`es`/`en`) and the
 * generation it came from.
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

        /** The patient's free question: empty = no question; too short or too long = invalid. */
        fun ownQuestion(raw: String): OwnQuestion {
            val trimmed = raw.trim()
            return when {
                trimmed.isEmpty() -> OwnQuestion.Empty
                trimmed.length < MIN_LENGTH -> OwnQuestion.TooShort
                trimmed.length > MAX_LENGTH -> OwnQuestion.TooLong
                else -> OwnQuestion.Valid(CheckInQuestion(trimmed, QuestionOrigin.PATIENT))
            }
        }

        /** An accepted AI suggestion, with its language (if not `es`/`en`, it is sent without one). */
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
