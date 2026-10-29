package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.core.designsystem.component.UiText
import java.time.Instant

/**
 * My plan.
 *
 * @param plan the current version; `null` while loading, without targets ([hasNoTargets]) or on failure with no copy.
 * @param cachedAt when the copy being shown was saved (offline); `null` if it comes from the backend.
 * @param loadFailed error with no saved copy: error state with "Retry".
 */
data class MyPlanUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val plan: PlanDetails? = null,
    val cachedAt: Instant? = null,
    val hasNoTargets: Boolean = false,
    val loadFailed: Boolean = false,
    val isAcknowledging: Boolean = false,
    val showServerError: Boolean = false,
)

/**
 * The current version ready to render: codes already turned into [UiText] (translatable) and text written by a
 * person as `UiText.Raw` (as is).
 *
 * @param proteinShare share of daily energy each macro provides (the "Daily targets" bars).
 */
data class PlanDetails(
    val planVersion: Int,
    val publishedAt: Instant,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val proteinShare: Float,
    val carbShare: Float,
    val fatShare: Float,
    val guidelines: List<UiText>,
    val restrictions: List<UiText>,
    val changes: List<PlanChangeLine>,
    val patientMessage: String?,
)

/** A line of "What changed in this version"; [secondary] = supporting text ("The targets … did not change"). */
data class PlanChangeLine(val text: UiText, val secondary: Boolean)

sealed interface MyPlanEvent {
    /** "I've reviewed them" → Home. */
    data object NavigateBack : MyPlanEvent
}
