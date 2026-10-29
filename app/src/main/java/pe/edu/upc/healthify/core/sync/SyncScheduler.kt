package pe.edu.upc.healthify.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Requests that the pending queue be sent as soon as there is network. */
fun interface SyncScheduler {
    fun requestSync()
}

/**
 * Unique work with a network constraint and exponential backoff (30 s, 1 min, 2 min… up to the
 * WorkManager maximum). `APPEND_OR_REPLACE`: if a run is already in progress, the new one runs after it and picks
 * up what was queued in the meantime.
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
