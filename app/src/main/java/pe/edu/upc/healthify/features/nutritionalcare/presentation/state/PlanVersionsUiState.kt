package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.core.designsystem.component.UiText
import java.time.Instant

/**
 * Previous versions (read-only). Offline there is no list: `plan-versions` is not stored on the
 * phone (the current version is, for My plan).
 *
 * @param selectedVersion the version whose detail is open (bottom sheet); `null` with no detail.
 */
data class PlanVersionsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val versions: List<PlanVersionItem> = emptyList(),
    val loadFailed: Boolean = false,
    val selectedVersion: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && !loadFailed && !isOffline && versions.isEmpty()

    val selected: PlanVersionItem? get() = selectedVersion?.let { v -> versions.firstOrNull { it.version == v } }
}

/**
 * A published version ready to render, in the list and its detail (for the patient and the nutritionist). The
 * codes go as translatable [UiText] and what the nutritionist wrote as `UiText.Raw`.
 */
data class PlanVersionItem(
    val version: Int,
    val publishedAt: Instant,
    val energyKcal: Double,
    val isActive: Boolean,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val guidelines: List<UiText> = emptyList(),
    val restrictions: List<UiText> = emptyList(),
    val changes: List<PlanChangeLine> = emptyList(),
    val patientMessage: String? = null,
)
