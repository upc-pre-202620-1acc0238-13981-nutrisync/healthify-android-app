package pe.edu.upc.healthify.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic offline queue. The feature repositories (diary, self weigh-ins) use it from their
 * infrastructure: they queue what was recorded offline and [PendingSyncWorker] sends it when there is network.
 */
@Singleton
class PendingOperationQueue @Inject constructor(
    private val dao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
) {

    /**
     * Stores the operation and requests a sync. Returns the `clientEntryId`, which the feature must include
     * in its [payload] if the endpoint expects it in the body. Queuing the same id twice does not duplicate it.
     */
    suspend fun enqueue(
        type: String,
        ownerUserId: Long,
        payload: String,
        localTimestamp: String,
        clientEntryId: String = newClientEntryId(),
    ): String {
        dao.insert(
            PendingOperationEntity(
                clientEntryId = clientEntryId,
                type = type,
                ownerUserId = ownerUserId,
                payload = payload,
                localTimestamp = localTimestamp,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        syncScheduler.requestSync()
        return clientEntryId
    }

    /** Pending and rejected operations of one type, in the order they were recorded. */
    fun observe(ownerUserId: Long, type: String): Flow<List<PendingOperation>> =
        dao.observe(ownerUserId, type).map { entities -> entities.map { it.toModel() } }

    fun observePendingCount(ownerUserId: Long): Flow<Int> = dao.observePendingCount(ownerUserId)

    companion object {
        fun newClientEntryId(): String = UUID.randomUUID().toString()
    }
}
