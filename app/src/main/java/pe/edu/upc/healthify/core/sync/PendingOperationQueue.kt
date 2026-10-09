package pe.edu.upc.healthify.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cola offline genérica. La usan los repositorios de las features (diario, autopesajes) desde
 * su infrastructure: encolan lo registrado sin conexión y [PendingSyncWorker] lo envía cuando hay red.
 */
@Singleton
class PendingOperationQueue @Inject constructor(
    private val dao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
) {

    /**
     * Guarda la operación y pide una sincronización. Devuelve el `clientEntryId`, que la feature debe incluir
     * en su [payload] si el endpoint lo pide en el cuerpo. Encolar dos veces el mismo id no la duplica.
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

    /** Pendientes y rechazadas de un tipo, en el orden en que se registraron (PT19). */
    fun observe(ownerUserId: Long, type: String): Flow<List<PendingOperation>> =
        dao.observe(ownerUserId, type).map { entities -> entities.map { it.toModel() } }

    fun observePendingCount(ownerUserId: Long): Flow<Int> = dao.observePendingCount(ownerUserId)

    companion object {
        fun newClientEntryId(): String = UUID.randomUUID().toString()
    }
}
