package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummary

/**
 * Your week (with the no-summary-yet state). The summary is not stored on the phone: offline there is nothing to
 * show.
 */
data class WeeklySummaryUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val summary: WeeklySummary? = null,
    /** No summary yet: `404 NotEnoughData`. */
    val notYet: Boolean = false,
    /** The feature is off (`403`/`503 AiFeatureDisabled`). */
    val isOff: Boolean = false,
    val loadFailed: Boolean = false,
)
