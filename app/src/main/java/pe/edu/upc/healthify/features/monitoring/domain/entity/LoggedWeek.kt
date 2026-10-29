package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.LocalDate

/** Un día del rango de `daily-compliance` (MA-6): solo su resultado, sin energía. */
data class ComplianceDay(val date: LocalDate, val outcome: ComplianceOutcome) {
    /** Registrado = cualquier resultado distinto de «sin registro», como [DailyProgress.isLogged]. */
    val isLogged: Boolean get() = outcome != ComplianceOutcome.UNLOGGED
}

/**
 * PT3 «Tu semana»: los últimos 7 días hasta [today], del más antiguo al de hoy. Solo dice qué días tienen registro:
 * un día sin registro nunca es una alerta.
 */
data class LoggedWeek(val today: LocalDate, val days: List<ComplianceDay>) {
    init {
        require(days.size == DAYS) { "A week has $DAYS days" }
    }

    val loggedDays: Int get() = days.count { it.isLogged }

    companion object {
        const val DAYS = 7

        /** Los 7 días hasta [today]; un día que el backend no devolvió queda «sin registro» (nunca se inventa). */
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
