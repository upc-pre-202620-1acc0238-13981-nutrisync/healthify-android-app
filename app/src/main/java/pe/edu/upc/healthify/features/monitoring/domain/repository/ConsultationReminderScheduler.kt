package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder

/** Local reminders for upcoming visits on this phone (there is no push: the phone schedules them). */
interface ConsultationReminderScheduler {

    /** Replaces all scheduled reminders with [reminders], for user [userId]. */
    fun replaceAll(userId: Long, reminders: List<ConsultationReminder>)

    /** Removes all reminders (signed out). */
    fun cancelAll()

    /** Asks to re-read upcoming visits in the background, with network, and reschedule the reminders. */
    fun requestRefresh()
}
