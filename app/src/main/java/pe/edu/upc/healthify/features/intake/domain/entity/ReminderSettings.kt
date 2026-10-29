package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The patient's local reminders (phone settings, nothing goes to the backend). The notification tone is
 * neutral, never pushy ("Time for your self weigh-in").
 *
 * The times are the ones in the design (7:30 a.m.; 1:00 p.m. and 8:00 p.m.); the design does not let them change.
 */
enum class ReminderKind(val times: List<LocalTime>) {
    SELF_WEIGH_IN(listOf(LocalTime.of(7, 30))),
    MEALS(listOf(LocalTime.of(13, 0), LocalTime.of(20, 0))),
}

/** Both start off: a notification only arrives if the patient asked for it. */
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
         * The next time a notification is due at [time] (today if it has not passed yet, otherwise tomorrow), in the
         * zone of [now]. Across a daylight saving change the nearest valid local time is used.
         */
        fun nextOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
            val today = now.toLocalDate().atTime(time).atZone(now.zone)
            return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        }
    }
}
