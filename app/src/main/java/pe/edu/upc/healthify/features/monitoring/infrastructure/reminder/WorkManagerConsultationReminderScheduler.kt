package pe.edu.upc.healthify.features.monitoring.infrastructure.reminder

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationReminderScheduler
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One unique WorkManager job per reminder (a day and an hour before each visit), plus a periodic re-read of the
 * agenda to follow changes made from another phone.
 *
 * The backend has no push; the phone schedules the reminders from what it last read. If the visit moves on another
 * device, the reminder is corrected on the next re-read (on app start or every [REFRESH_HOURS] h).
 * As with the other local reminders, WorkManager may delay it a few minutes to save battery.
 */
@Singleton
class WorkManagerConsultationReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
) : ConsultationReminderScheduler {

    override fun replaceAll(userId: Long, reminders: List<ConsultationReminder>) {
        // WorkManager applies operations in order: old reminders are removed first, then the new ones are scheduled.
        workManager.cancelAllWorkByTag(TAG_NOTIFY)
        val now = clock.instant()
        reminders.forEach { reminder ->
            reminder.triggersAfter(now).forEach { trigger ->
                val request = OneTimeWorkRequestBuilder<ConsultationReminderWorker>()
                    .setInitialDelay(Duration.between(now, trigger.at).toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            ConsultationReminderWorker.KEY_USER_ID to userId,
                            ConsultationReminderWorker.KEY_FOLLOW_UP_ID to reminder.followUpId,
                            ConsultationReminderWorker.KEY_SCHEDULED_FOR to reminder.scheduledFor.toEpochMilli(),
                            ConsultationReminderWorker.KEY_LEAD_MINUTES to trigger.lead.toMinutes(),
                            ConsultationReminderWorker.KEY_IS_PRACTITIONER to reminder.forPractitioner,
                            ConsultationReminderWorker.KEY_PATIENT_NAME to reminder.patientFullName,
                        ),
                    )
                    .addTag(TAG_NOTIFY)
                    .build()
                workManager.enqueueUniqueWork(
                    "consultation-reminder-${reminder.followUpId}-${trigger.lead.toMinutes()}",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }
        }
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_NOTIFY)
        workManager.cancelUniqueWork(PERIODIC_REFRESH)
    }

    override fun requestRefresh() {
        val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val now = OneTimeWorkRequestBuilder<ConsultationRemindersRefreshWorker>()
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(REFRESH, ExistingWorkPolicy.REPLACE, now)
        val periodic = PeriodicWorkRequestBuilder<ConsultationRemindersRefreshWorker>(REFRESH_HOURS, TimeUnit.HOURS)
            .setConstraints(online)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_REFRESH, ExistingPeriodicWorkPolicy.KEEP, periodic)
    }

    private companion object {
        const val TAG_NOTIFY = "consultation-reminder"
        const val REFRESH = "consultation-reminders-refresh"
        const val PERIODIC_REFRESH = "consultation-reminders-periodic-refresh"
        const val REFRESH_HOURS = 6L
        const val INITIAL_BACKOFF_SECONDS = 30L
    }
}
