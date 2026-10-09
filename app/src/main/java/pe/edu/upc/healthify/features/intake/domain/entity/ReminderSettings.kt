package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * PT22 · los recordatorios locales del paciente (configuración del teléfono, nada va al backend). El tono de los avisos
 * es neutro, nunca de presión («Es hora de tu autopesaje»).
 *
 * DECISIÓN PT22: las horas son las del frame (7:30 a. m.; 1:00 p. m. y 8:00 p. m.); el Figma no permite cambiarlas.
 */
enum class ReminderKind(val times: List<LocalTime>) {
    SELF_WEIGH_IN(listOf(LocalTime.of(7, 30))),
    MEALS(listOf(LocalTime.of(13, 0), LocalTime.of(20, 0))),
}

/** Ambos empiezan apagados: un aviso solo llega si el paciente lo pidió. */
data class ReminderSettings(
    val selfWeighInEnabled: Boolean = false,
    val mealsEnabled: Boolean = false,
) {
    fun isEnabled(kind: ReminderKind): Boolean = when (kind) {
        ReminderKind.SELF_WEIGH_IN -> selfWeighInEnabled
        ReminderKind.MEALS -> mealsEnabled
    }

    fun with(kind: ReminderKind, enabled: Boolean): ReminderSettings = when (kind) {
        ReminderKind.SELF_WEIGH_IN -> copy(selfWeighInEnabled = enabled)
        ReminderKind.MEALS -> copy(mealsEnabled = enabled)
    }

    companion object {
        /**
         * La próxima vez que toca un aviso a la hora [time] (hoy si todavía no pasó, si no mañana), en la zona de
         * [now]. Con el cambio de horario se usa la hora local válida más cercana.
         */
        fun nextOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
            val today = now.toLocalDate().atTime(time).atZone(now.zone)
            return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        }
    }
}
