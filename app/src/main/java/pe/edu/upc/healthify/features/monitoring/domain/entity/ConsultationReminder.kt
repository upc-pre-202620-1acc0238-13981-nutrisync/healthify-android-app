package pe.edu.upc.healthify.features.monitoring.domain.entity

import java.time.Duration
import java.time.Instant

/**
 * Local reminder for a scheduled visit, for the patient and their nutritionist: a day before and an hour before. It
 * only carries the time and, for the nutritionist, the patient's name; never clinical data.
 *
 * @param forPractitioner the reminder is for the nutritionist (otherwise for the patient).
 * @param patientFullName the visit's patient (only in the nutritionist's reminders).
 */
data class ConsultationReminder(
    val followUpId: Long,
    val scheduledFor: Instant,
    val forPractitioner: Boolean = false,
    val patientFullName: String? = null,
) {
    /** Reminders that have not passed yet, soonest first. */
    fun triggersAfter(now: Instant): List<ReminderTrigger> =
        LEADS.map { ReminderTrigger(it, scheduledFor.minus(it)) }
            .filter { it.at.isAfter(now) }
            .sortedBy { it.at }

    companion object {
        /** How far ahead the reminder fires. */
        val LEADS: List<Duration> = listOf(Duration.ofHours(24), Duration.ofHours(1))
    }
}

/** One [ConsultationReminder] reminder: [lead] before the visit, at [at]. */
data class ReminderTrigger(val lead: Duration, val at: Instant)
