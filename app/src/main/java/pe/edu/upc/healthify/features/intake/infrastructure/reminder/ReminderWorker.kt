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
 * Posts the local notification if, when the time comes, there is a session and that patient has the reminder on.
 * With no session, the reminder off or no notification permission it does nothing (the work stays scheduled).
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
