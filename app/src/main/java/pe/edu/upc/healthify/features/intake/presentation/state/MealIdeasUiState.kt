package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome

/** Por qué no hay ideas que mostrar (cada uno con su texto; nunca acusatorio). */
enum class MealIdeasFailure {
    /** PT14.4.E · 5xx, 502 `AiOutputRejected`, 503 `AiProviderUnavailable`. */
    GENERIC,

    /** `422 NotEnoughRemaining`: «Ya cubriste tu energía de hoy». */
    NOT_ENOUGH_REMAINING,

    /** `404 ActiveTargetsCacheNotFound`. */
    NO_TARGETS,

    /** `403 AiConsentRequired` o `503 AiFeatureDisabled`. */
    UNAVAILABLE,

    /** `429 AiRateLimited`. */
    RATE_LIMITED,
}

/**
 * PT14.4 Ideas para hoy (+ PT14.4.E, PT14.4.O) y PT14.5 Detalle (si [selectedIdeaId] no es nulo).
 *
 * @param isOffline sin conexión y sin ideas: PT14.4.O.
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
