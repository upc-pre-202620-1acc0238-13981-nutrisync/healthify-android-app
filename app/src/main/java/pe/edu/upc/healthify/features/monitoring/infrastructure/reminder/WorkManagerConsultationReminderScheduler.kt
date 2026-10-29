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
 * Un trabajo único de WorkManager por aviso (un día y una hora antes de cada consulta), más una relectura periódica de
 * la agenda para seguir los cambios que se hagan desde otro teléfono.
 *
 * DECISIÓN: no hay push en el backend; los avisos los programa el teléfono con lo que leyó la última vez. Si la consulta
 * se mueve en otro dispositivo, el aviso se corrige en la siguiente relectura (al abrir la app o cada [REFRESH_HOURS] h).
 * Como en PT22, WorkManager puede diferir el aviso unos minutos para ahorrar batería.
 */
@Singleton
class WorkManagerConsultationReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
) : ConsultationReminderScheduler {

    override fun replaceAll(userId: Long, reminders: List<ConsultationReminder>) {
        // WorkManager aplica las operaciones en orden: primero se quitan los avisos viejos, luego se programan los nuevos.
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
