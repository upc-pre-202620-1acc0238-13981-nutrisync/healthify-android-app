package pe.edu.upc.healthify.features.intake.infrastructure.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderScheduler
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Un trabajo diario de WorkManager por hora de aviso (sobrevive a reinicios, sin `AlarmManager` exacto).
 *
 * DECISIÓN PT22: el aviso llega aproximadamente a la hora indicada (WorkManager puede diferirlo unos minutos para
 * ahorrar batería); los avisos son del teléfono, no de la cuenta: [ReminderWorker] mira al llegar si el paciente con
 * sesión lo tiene encendido.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {

    override fun update(kind: ReminderKind, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        kind.times.forEach { time ->
            val name = workName(kind, time)
            if (!enabled) {
                workManager.cancelUniqueWork(name)
                return@forEach
            }
            val now = ZonedDateTime.now(clock)
            val delay = Duration.between(now, ReminderSettings.nextOccurrence(time, now))
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to kind.name))
                .addTag(TAG)
                .build()
            workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
        }
    }

    private fun workName(kind: ReminderKind, time: LocalTime) = "reminder-${kind.name}-${time.hour}-${time.minute}"

    private companion object {
        const val TAG = "local-reminder"
    }
}
