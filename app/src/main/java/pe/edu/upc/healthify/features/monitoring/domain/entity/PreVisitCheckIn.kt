package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.QuestionOrigin
import java.time.Instant

/**
 * "Tell them how it went" as the backend saved it (`PreVisitCheckInResource`). One per visit; the patient can edit
 * it until the visit's time ([isLocked] comes from the backend). It is voluntary and raises no signals.
 */
data class PreVisitCheckIn(
    val followUpId: FollowUpId,
    val answer: CheckInAnswer,
    val submittedAt: Instant,
    val editedAt: Instant?,
    val isLocked: Boolean,
)

/**
 * What the patient answers (`SubmitPreVisitCheckInResource`): how they felt (required), difficulties (no repeats)
 * and up to 3 custom questions and 3 accepted AI suggestions.
 */
data class CheckInAnswer(
    val feeling: PlanFeeling,
    val difficulties: Set<CheckInDifficulty> = emptySet(),
    val questions: List<CheckInQuestion> = emptyList(),
) {
    init {
        require(questions.distinct().size == questions.size) { "A check in does not repeat a question" }
        require(questions.groupBy { it.origin }.values.all { it.size <= MAX_QUESTIONS_PER_ORIGIN }) {
            "A check in has at most $MAX_QUESTIONS_PER_ORIGIN questions of each origin"
        }
    }

    val ownQuestions: List<CheckInQuestion> get() = questions.filter { it.origin == QuestionOrigin.PATIENT }
    val aiQuestions: List<CheckInQuestion> get() = questions.filter { it.origin == QuestionOrigin.AI_SUGGESTED }

    companion object {
        const val MAX_QUESTIONS_PER_ORIGIN = 3
    }
}

/** Check-in summary inside My visits (`ConsultationCheckInSummaryResource`): questions as text only. */
data class CheckInSummary(
    val feeling: PlanFeeling,
    val difficulties: Set<CheckInDifficulty>,
    val questions: List<String>,
    val submittedAt: Instant,
    val editedAt: Instant?,
    val isLocked: Boolean,
)

/** AI-suggested questions to bring to the visit (`SuggestedQuestionsResource`). */
data class SuggestedQuestions(
    val questions: List<SuggestedQuestion>,
    val aiGenerationId: Long,
    /** Language they were generated in; sent with each accepted suggestion. */
    val language: String?,
)

data class SuggestedQuestion(val id: String, val text: String) {
    init {
        require(text.isNotBlank()) { "A suggested question needs text" }
    }
}

/** The questions, "not yet" (`404 NotEnoughData`) or the feature off (`403`/`503 AiFeatureDisabled`). */
sealed interface SuggestedQuestionsAvailability {
    data class Ready(val suggestions: SuggestedQuestions) : SuggestedQuestionsAvailability
    data object NotYet : SuggestedQuestionsAvailability
    data object Off : SuggestedQuestionsAvailability
}
