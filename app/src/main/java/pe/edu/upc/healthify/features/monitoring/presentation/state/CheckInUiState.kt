package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling

/** Tell them how it went (with the send-error state). */
data class CheckInUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    /** There was already a reply: it is being edited ("Edit my reply"). */
    val isEditing: Boolean = false,
    val feeling: PlanFeeling? = null,
    /** "Pick how you felt" (the feeling is required). */
    val feelingMissing: Boolean = false,
    val difficulties: Set<CheckInDifficulty> = emptySet(),
    /** The patient's free question, as typed (never translated). */
    val ownQuestion: String = "",
    val ownQuestionError: OwnQuestionError? = null,
    /** "You could also ask": empty = the card does not appear. */
    val suggestions: List<SuggestionOption> = emptyList(),
    val isSubmitting: Boolean = false,
    /** Send error: network or server; what was written is kept. */
    val submitFailed: Boolean = false,
    /** The visit's time arrived (or the backend locked it): it can no longer be sent or edited. */
    val isLocked: Boolean = false,
    /** The visit is no longer scheduled (moved or cancelled). */
    val notScheduled: Boolean = false,
) {
    /** Up to 3 AI suggestions per check-in: with 3 picked, the rest cannot be selected. */
    val canSelectMoreSuggestions: Boolean get() = suggestions.count { it.selected } < MAX_AI_QUESTIONS

    val canSubmit: Boolean get() = !isLoading && !loadFailed && !isSubmitting && !isLocked && !notScheduled

    companion object {
        const val MAX_AI_QUESTIONS = 3
        const val MAX_QUESTION_LENGTH = 300
    }
}

enum class OwnQuestionError { TOO_SHORT, TOO_LONG }

/** An AI suggestion: added with its language and the generation it came from. */
data class SuggestionOption(
    val text: String,
    val selected: Boolean,
    val aiGenerationId: Long?,
    val language: String?,
)
