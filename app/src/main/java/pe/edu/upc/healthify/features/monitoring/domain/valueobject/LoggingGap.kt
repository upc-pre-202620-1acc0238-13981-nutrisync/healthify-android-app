package pe.edu.upc.healthify.features.monitoring.domain.valueobject

import java.time.LocalDate

/**
 * Logging gap ("We miss you around here"). The backend detects it on its own clock but does not expose it over
 * REST (the reminder is local), so the phone applies the same rule:
 * `today − last logged day ≥ 3` (`Monitoring:LoggingGapThresholdDays`).
 *
 * The days are read from `daily-compliance?from=&to=` (the [THRESHOLD_DAYS] days up to and including today);
 * there is only a gap if tracking had already run that long (`window.From` is not exposed, so the date the current
 * goals took effect is used). That way a newly linked patient does not get a "you haven't logged in a few days".
 *
 * A gap is never a deviation and never escalates: it only invites logging (*Gap Is Not A Deviation*).
 */
object LoggingGap {

    const val THRESHOLD_DAYS = 3L

    /** First day of the queried range: including today, that makes [THRESHOLD_DAYS] days. */
    fun lookbackStart(today: LocalDate): LocalDate = today.minusDays(THRESHOLD_DAYS - 1)

    /**
     * @param loggedDaysInLookback days with at least one entry between [lookbackStart] and [today].
     * @param trackingSince since when the patient can be asked to log.
     */
    fun isGap(loggedDaysInLookback: Int, trackingSince: LocalDate, today: LocalDate): Boolean {
        require(loggedDaysInLookback >= 0) { "Day counts cannot be negative" }
        val trackedLongEnough = !trackingSince.isAfter(today.minusDays(THRESHOLD_DAYS))
        return trackedLongEnough && loggedDaysInLookback == 0
    }
}
