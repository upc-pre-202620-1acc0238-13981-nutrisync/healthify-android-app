package pe.edu.upc.healthify.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Pide que la cola de pendientes se envíe en cuanto haya red. */
fun interface SyncScheduler {
    fun requestSync()
}

/**
 * Trabajo único con restricción de red y backoff exponencial (30 s, 1 min, 2 min… hasta el máximo de
 * WorkManager). `APPEND_OR_REPLACE`: si ya hay una ejecución en curso, la nueva corre después y recoge lo que se
 * encoló mientras tanto.
 */
class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<PendingSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "pending-operations-sync"
        const val INITIAL_BACKOFF_SECONDS = 30L
    }
}
