package pe.edu.upc.healthify.features.monitoring.domain.entity

import java.time.Instant
import java.time.LocalDate

/**
 * The week's figures, computed without AI. A day without logs does **not** count as a missed goal
 * (`unloggedDays` is separate from `metDays`).
 */
data class WeeklySummaryFacts(
    val metDays: Int,
    val totalDays: Int,
    val loggedDays: Int,
    val unloggedDays: Int,
    val weightChangeKg: Double?,
) {
    init {
        require(totalDays > 0) { "A week has days" }
        require(metDays in 0..totalDays && loggedDays in 0..totalDays && unloggedDays in 0..totalDays) {
            "Day counts must be within the week"
        }
        require(metDays <= loggedDays) { "A day within the targets is a logged day" }
    }
}

/**
 * AI weekly summary (the "Your week" card and screen). The AI writes it every Monday from the previous week's diary,
 * already in the reader's language; it is shown **as is** (the app does not translate it).
 *
 * @param wentWell "What went well": 1 to 3 bullets.
 * @param watchOut "What to watch": up to 2, phrased as an invitation.
 */
data class WeeklySummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val headline: String,
    val wentWell: List<String>,
    val watchOut: List<String>,
    val facts: WeeklySummaryFacts,
    val generatedAt: Instant,
) {
    init {
        require(headline.isNotBlank()) { "A weekly summary has a headline" }
        require(!weekEnd.isBefore(weekStart)) { "The week is inverted" }
        require(wentWell.none { it.isBlank() } && watchOut.none { it.isBlank() }) { "Bullets cannot be blank" }
    }
}

/** What is available for the "Your week" card and screen. */
sealed interface WeeklySummaryAvailability {

    data class Ready(val summary: WeeklySummary) : WeeklySummaryAvailability

    /** "No summary yet": `404 NotEnoughData` (few days logged or not generated yet). */
    data object NotYet : WeeklySummaryAvailability

    /**
     * The feature is off for this patient or there is no AI consent (`403`), or the server's AI is off
     * (`503 AiFeatureDisabled`): the card does not appear.
     */
    data object Off : WeeklySummaryAvailability
}
