package pe.edu.upc.healthify.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Sends the pending queue (diary entries and self weigh-ins). Scheduled by [WorkManagerSyncScheduler]. */
@HiltWorker
class PendingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: PendingSyncEngine,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (engine.syncAll()) {
        SyncRunResult.Done -> Result.success()
        SyncRunResult.Retry -> Result.retry()
    }
}
