package pe.edu.upc.healthify.core.sync

import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import javax.inject.Inject

/**
 * Envía la cola de la sesión activa, tipo por tipo y en lotes, con el [PendingOperationSender] de cada tipo.
 * Separado del worker para probarlo en la JVM.
 */
class PendingSyncEngine @Inject constructor(
    private val dao: PendingOperationDao,
    senders: Set<@JvmSuppressWildcards PendingOperationSender>,
    private val syncEvents: SyncEvents,
    private val activeUserIdProvider: ActiveUserIdProvider,
) {

    private val sendersByType = senders.associateBy { it.type }

    suspend fun syncAll(): SyncRunResult {
        // Sin sesión no se envía nada; al volver a entrar se pide otra sincronización.
        val ownerUserId = activeUserIdProvider.activeUserId() ?: return SyncRunResult.Done
        var retry = false
        for (type in dao.pendingTypes(ownerUserId)) {
            // Un tipo sin sender (p. ej. de una versión más nueva) queda en la cola sin bloquear al resto.
            val sender = sendersByType[type] ?: continue
            if (!syncType(ownerUserId, sender)) retry = true
        }
        return if (retry) SyncRunResult.Retry else SyncRunResult.Done
    }

    /** `true` si se vació la cola de ese tipo; `false` si quedó algo para reintentar. */
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
                    // Lo que el backend no reportó sigue pendiente: se reintenta en la próxima ejecución
                    // (no en este bucle, que volvería a leer el mismo lote).
                    if (ids.any { it !in result.accepted && it !in result.rejected }) return false
                }
            }
        }
    }
}

enum class SyncRunResult { Done, Retry }
