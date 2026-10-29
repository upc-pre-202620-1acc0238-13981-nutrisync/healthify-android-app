package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome

/** Why there are no ideas to show (each with its own text; never accusing). */
enum class MealIdeasFailure {
    /** 5xx, 502 `AiOutputRejected`, 400 and any other error. */
    GENERIC,

    /** `422 NotEnoughRemaining`: "You already covered today's energy". */
    NOT_ENOUGH_REMAINING,

    /** `404 ActiveTargetsCacheNotFound`. */
    NO_TARGETS,

    /** `503 AiFeatureDisabled` or `503 AiProviderUnavailable`: the AI is not available right now. */
    UNAVAILABLE,

    /** `403 AiConsentRequired`: the AI permission is missing or the ideas feature is off. */
    NO_CONSENT,

    /** `429 AiRateLimited` (daily quota) or `429 TooManyRequests` (too many AI requests in a row). */
    RATE_LIMITED,
}

/**
 * Ideas for today (with its error and offline states) and the idea detail (if [selectedIdeaId] is not null).
 *
 * @param isOffline offline and with no ideas.
 */
data class MealIdeasUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val ideas: MealIdeas? = null,
    val failure: MealIdeasFailure? = null,
    val isLoadingMore: Boolean = false,
    val selectedIdeaId: String? = null,
    val isRegistering: Boolean = false,
    val showRegisterError: Boolean = false,
    val showMoreError: Boolean = false,
) {
    val selectedIdea: MealIdea? get() = selectedIdeaId?.let { id -> ideas?.ideas?.firstOrNull { it.id == id } }
}

sealed interface MealIdeasEvent {
    data class Logged(val outcome: MealLogOutcome) : MealIdeasEvent
}
