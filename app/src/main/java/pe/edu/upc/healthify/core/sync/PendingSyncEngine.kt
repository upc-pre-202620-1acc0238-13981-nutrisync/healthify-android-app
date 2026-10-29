package pe.edu.upc.healthify.core.sync

import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import javax.inject.Inject

/**
 * Sends the active session's queue, type by type and in batches, with each type's [PendingOperationSender].
 * Kept apart from the worker so it can be tested on the JVM.
 */
class PendingSyncEngine @Inject constructor(
    private val dao: PendingOperationDao,
    senders: Set<@JvmSuppressWildcards PendingOperationSender>,
    private val syncEvents: SyncEvents,
    private val activeUserIdProvider: ActiveUserIdProvider,
) {

    private val sendersByType = senders.associateBy { it.type }

    suspend fun syncAll(): SyncRunResult {
        // Without a session nothing is sent; signing in again requests another sync.
        val ownerUserId = activeUserIdProvider.activeUserId() ?: return SyncRunResult.Done
        var retry = false
        for (type in dao.pendingTypes(ownerUserId)) {
            // A type without a sender (e.g. from a newer version) stays in the queue without blocking the rest.
            val sender = sendersByType[type] ?: continue
            if (!syncType(ownerUserId, sender)) retry = true
        }
        return if (retry) SyncRunResult.Retry else SyncRunResult.Done
    }

    /** `true` if that type's queue was emptied; `false` if something was left to retry. */
    private suspend fun syncType(ownerUserId: Long, sender: PendingOperationSender): Boolean {
        while (true) {
            val batch = dao.pendingBatch(ownerUserId, sender.type, sender.maxBatchSize)
            if (batch.isEmpty()) return true
            val ids = batch.map { it.clientEntryId }
            dao.incrementAttempts(ids)
            when (val result = sender.send(ownerUserId, batch.map { it.toModel() })) {
                SendBatchResult.RetryLater -> return false
                is SendBatchResult.Delivered -> {
                    val accepted = ids.filter { it in result.accepted }
                    if (accepted.isNotEmpty()) {
                        dao.delete(accepted)
                        syncEvents.notifyDelivered(sender.type)
                    }
                    ids.forEach { id -> result.rejected[id]?.let { reason -> dao.markRejected(id, reason) } }
                    // What the backend did not report stays pending: it is retried on the next run
                    // (not in this loop, which would read the same batch again).
                    if (ids.any { it !in result.accepted && it !in result.rejected }) return false
                }
            }
        }
    }
}

enum class SyncRunResult { Done, Retry }
