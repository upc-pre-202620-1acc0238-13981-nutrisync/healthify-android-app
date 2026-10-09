package pe.edu.upc.healthify.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity

/** [PendingOperationDao] en memoria con la misma semántica que las queries de Room. */
class FakePendingOperationDao : PendingOperationDao {

    val rows = MutableStateFlow<List<PendingOperationEntity>>(emptyList())

    override suspend fun insert(operation: PendingOperationEntity): Long {
        if (rows.value.any { it.clientEntryId == operation.clientEntryId }) return -1
        rows.value = rows.value + operation
        return rows.value.size.toLong()
    }

    override suspend fun pendingTypes(ownerUserId: Long): List<String> =
        pending(ownerUserId).map { it.type }.distinct()

    override suspend fun pendingBatch(ownerUserId: Long, type: String, limit: Int): List<PendingOperationEntity> =
        pending(ownerUserId).filter { it.type == type }.sortedBy { it.createdAtEpochMs }.take(limit)

    override suspend fun incrementAttempts(clientEntryIds: List<String>) = update(clientEntryIds) {
        it.copy(attempts = it.attempts + 1)
    }

    override suspend fun delete(clientEntryIds: List<String>) {
        rows.value = rows.value.filterNot { it.clientEntryId in clientEntryIds }
    }

    override suspend fun markRejected(clientEntryId: String, rejectionCode: String) = update(listOf(clientEntryId)) {
        it.copy(status = PendingOperationEntity.STATUS_REJECTED, rejectionCode = rejectionCode)
    }

    override fun observe(ownerUserId: Long, type: String): Flow<List<PendingOperationEntity>> =
        rows.map { all -> all.filter { it.ownerUserId == ownerUserId && it.type == type } }

    override fun observePendingCount(ownerUserId: Long): Flow<Int> =
        rows.map { all -> all.count { it.ownerUserId == ownerUserId && it.status == PendingOperationEntity.STATUS_PENDING } }

    fun row(clientEntryId: String): PendingOperationEntity? = rows.value.firstOrNull { it.clientEntryId == clientEntryId }

    private fun pending(ownerUserId: Long) =
        rows.value.filter { it.ownerUserId == ownerUserId && it.status == PendingOperationEntity.STATUS_PENDING }

    private fun update(ids: List<String>, transform: (PendingOperationEntity) -> PendingOperationEntity) {
        rows.value = rows.value.map { if (it.clientEntryId in ids) transform(it) else it }
    }
}
