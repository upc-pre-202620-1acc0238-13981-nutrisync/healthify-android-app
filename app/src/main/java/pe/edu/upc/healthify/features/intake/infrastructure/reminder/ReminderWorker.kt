package pe.edu.upc.healthify.features.intake.infrastructure.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pe.edu.upc.healthify.core.sync.ActiveUserIdProvider
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository

/**
 * PT22 · publica el aviso local si, al llegar la hora, hay sesión y ese paciente tiene el recordatorio encendido. Sin
 * sesión, con el recordatorio apagado o sin permiso de notificaciones no hace nada (el trabajo sigue programado).
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val activeUserIdProvider: ActiveUserIdProvider,
    private val settingsRepository: ReminderSettingsRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }
            ?: return Result.success()
        val userId = activeUserIdProvider.activeUserId() ?: return Result.success()
        if (settingsRepository.get(userId).isEnabled(kind)) notifier.show(kind)
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
    }
}
