package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.LocalDate

/** A day from the `daily-compliance` range: only its outcome, without energy. */
data class ComplianceDay(val date: LocalDate, val outcome: ComplianceOutcome) {
    /** Logged = any outcome other than "no log", like [DailyProgress.isLogged]. */
    val isLogged: Boolean get() = outcome != ComplianceOutcome.UNLOGGED
}

/**
 * Home "Your week": the last 7 days up to [today], oldest first. It only tells which days have logs:
 * a day without logs is never an alert.
 */
data class LoggedWeek(val today: LocalDate, val days: List<ComplianceDay>) {
    init {
        require(days.size == DAYS) { "A week has $DAYS days" }
    }

    val loggedDays: Int get() = days.count { it.isLogged }

    companion object {
        const val DAYS = 7

        /** The 7 days up to [today]; a day the backend did not return stays "no log" (never made up). */
        fun of(today: LocalDate, evaluated: List<ComplianceDay>): LoggedWeek {
            val byDate = evaluated.associateBy { it.date }
            val days = (DAYS - 1 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                byDate[date] ?: ComplianceDay(date, ComplianceOutcome.UNLOGGED)
            }
            return LoggedWeek(today, days)
        }
    }
}
