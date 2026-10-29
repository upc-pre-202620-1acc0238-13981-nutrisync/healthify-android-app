package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.LocalDate

/**
 * How a patient's day is going (`GET …/daily-compliance?date=`): the outcome and the **confirmed** energy logged
 * that day (only confirmed estimates are summed). A day the backend has not evaluated yet is [ComplianceOutcome.UNLOGGED]
 * with 0 kcal: "You haven't logged anything today yet".
 */
data class DailyProgress(
    val date: LocalDate,
    val outcome: ComplianceOutcome,
    val observedEnergyKcal: Double,
) {
    init {
        require(observedEnergyKcal >= 0) { "Observed energy cannot be negative" }
    }

    val isLogged: Boolean get() = outcome != ComplianceOutcome.UNLOGGED

    companion object {
        fun unlogged(date: LocalDate) = DailyProgress(date, ComplianceOutcome.UNLOGGED, observedEnergyKcal = 0.0)
    }
}

/**
 * Summary of a range of days (`DailyComplianceRangeResource.summary`). [totalDays] counts calendar
 * days; days without logs are in [unloggedDays] and never count against the patient.
 */
data class ComplianceSummary(
    val metDays: Int,
    val exceededDays: Int,
    val shortDays: Int,
    val unloggedDays: Int,
    val loggedDays: Int,
    val totalDays: Int,
) {
    init {
        require(listOf(metDays, exceededDays, shortDays, unloggedDays, loggedDays, totalDays).all { it >= 0 }) {
            "Day counts cannot be negative"
        }
        require(loggedDays <= totalDays) { "Logged days cannot exceed the range" }
    }
}
