package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import java.time.Instant

/**
 * Weight trend (with its Snackbar, empty and loading states). Never a "today's weight": only the smoothed series.
 *
 * @param trend the trend with at least 2 points in the range (otherwise [hasNoTrend]).
 * @param weeklyCard the "Your week" card; `null` = not shown (off, offline or with an error).
 */
data class WeightTrendUiState(
    val isLoading: Boolean = true,
    val trend: WeightTrend? = null,
    val hasNoTrend: Boolean = false,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val weeklyCard: WeeklyCard? = null,
    val weeks: Int,
) {
    /** Empty state (or `404 WeightTrendNotFound`, same copy). */
    val isEmpty: Boolean get() = !isLoading && hasNoTrend

    /** Offline with no copy on the phone: there is nothing to draw. */
    val isOfflineWithoutCopy: Boolean get() = !isLoading && isOffline && trend == null && !hasNoTrend && loadFailed

    /** "Some readings did not follow the protocol…". */
    val showExcluded: Boolean get() = (trend?.excludedReadingsCount ?: 0) > 0

    val cachedAt: Instant? get() = trend?.takeIf { it.fromCache }?.savedAt
}

/** The "Your week" card, already decided (the AI headline goes as is). */
sealed interface WeeklyCard {
    data class Ready(val headline: String) : WeeklyCard

    /** There is no summary yet; the card invites the patient and opens the empty state. */
    data object NotYet : WeeklyCard
}

sealed interface WeightTrendEvent {
    /** "Self weigh-in saved. The day's weight is not shown." (or "It will be sent…" offline). */
    data class ShowSaved(val outcome: SelfWeighInOutcome) : WeightTrendEvent
}
