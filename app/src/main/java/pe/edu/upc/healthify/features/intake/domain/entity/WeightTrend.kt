package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.Instant
import java.time.LocalDate

/** One point of the smoothed series: the moving average of the protocol self weigh-ins up to that day. */
data class WeightTrendPoint(val date: LocalDate, val smoothedKg: Double) {
    init {
        require(smoothedKg.isFinite() && smoothedKg > 0) { "A smoothed value must be a positive weight" }
    }
}

/**
 * Weight trend (read model *Weight Trend Chart*).
 *
 * Rule *Daily Figure Never Exposed As Headline*: this entity **has no** "latest weight" or "today's weight", just
 * like the backend resource. Only the smoothed series and its summary (slope and change), computed over that series.
 *
 * @param points the full series, from oldest to newest.
 * @param windowSize how many self weigh-ins are averaged into each point.
 * @param excludedReadingsCount self weigh-ins in the range without protocol: stored, but off the line.
 * @param changeKgOverRange last point of the range minus the first; `null` with fewer than two points.
 * @param fromCache comes from the phone's copy (offline), saved at [savedAt].
 */
data class WeightTrend(
    val points: List<WeightTrendPoint>,
    val windowSize: Int,
    val lastRecalculatedAt: Instant,
    val excludedReadingsCount: Int = 0,
    val changeKgOverRange: Double? = null,
    val slopeKgPerWeek: Double? = null,
    val rangeFrom: LocalDate? = null,
    val rangeTo: LocalDate? = null,
    val fromCache: Boolean = false,
    val savedAt: Instant? = null,
) {
    init {
        require(windowSize > 0) { "The moving average needs at least one reading" }
        require(excludedReadingsCount >= 0) { "Excluded readings cannot be negative" }
        require(points.zipWithNext().all { (a, b) -> a.date < b.date }) { "Points must be in date order, one per day" }
        require(rangeFrom == null || rangeTo == null || !rangeTo.isBefore(rangeFrom)) { "The range is inverted" }
        require(!fromCache || savedAt != null) { "A cached trend knows when it was saved" }
    }

    /** The points in the requested range (`?weeks=`); with no range, the whole series. */
    val rangePoints: List<WeightTrendPoint>
        get() = points.filter { point ->
            (rangeFrom == null || !point.date.isBefore(rangeFrom)) && (rangeTo == null || !point.date.isAfter(rangeTo))
        }

    /** At least 2 points in the range are needed to draw a line. */
    val canBeDrawn: Boolean get() = rangePoints.size >= MIN_POINTS_TO_DRAW

    /** Direction of the change over the range, with a tolerance of half a decimal (zero as displayed). */
    val direction: Direction?
        get() = changeKgOverRange?.let { change ->
            when {
                change >= STABLE_TOLERANCE_KG -> Direction.UP
                change <= -STABLE_TOLERANCE_KG -> Direction.DOWN
                else -> Direction.STABLE
            }
        }

    enum class Direction { UP, DOWN, STABLE }

    companion object {
        const val MIN_POINTS_TO_DRAW = 2

        /** A change shown as "0.0 kg" is called "stable". */
        const val STABLE_TOLERANCE_KG = 0.05
    }
}
