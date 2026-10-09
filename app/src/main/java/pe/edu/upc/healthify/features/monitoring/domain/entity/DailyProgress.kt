package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.LocalDate

/**
 * Cómo va un día del paciente (`GET …/daily-compliance?date=`): el resultado y la energía registrada **confirmada**
 * ese día (F21 suma solo estimaciones confirmadas). Un día que el backend aún no evaluó es [ComplianceOutcome.UNLOGGED]
 * con 0 kcal: «Todavía no registraste nada hoy».
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
 * Resumen de un rango de días (`DailyComplianceRangeResource.summary`, MA-6). [totalDays] cuenta los días de
 * calendario; los días sin registro están en [unloggedDays] y nunca restan.
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
