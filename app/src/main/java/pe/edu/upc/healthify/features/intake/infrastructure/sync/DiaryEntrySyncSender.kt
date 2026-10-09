package pe.edu.upc.healthify.features.intake.infrastructure.sync

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.core.sync.PendingOperation
import pe.edu.upc.healthify.core.sync.PendingOperationSender
import pe.edu.upc.healthify.core.sync.SendBatchResult
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toPendingDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.DiaryService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncPendingEntriesRequestDto
import javax.inject.Inject

/**
 * Envía las comidas encoladas a `POST /diary-entries/synchronization` (F20). Es idempotente por `clientEntryId`:
 * `Created`, `AlreadyPresent` y `ConflictResolved` salen de la cola; `Rejected` queda con su motivo a la vista
 * (PT19) y no se reintenta. Sin red, 5xx o sesión por renovar → se reintenta el lote más tarde.
 */
class DiaryEntrySyncSender @Inject constructor(
    private val service: DiaryService,
    private val json: Json,
) : PendingOperationSender {

    override val type: String = QueuedDiaryEntryPayload.TYPE

    override suspend fun send(ownerUserId: Long, operations: List<PendingOperation>): SendBatchResult {
        val unreadable = mutableMapOf<String, String>()
        val entries = operations.mapNotNull { operation ->
            val payload = decode(operation.payload)
            if (payload == null) unreadable[operation.clientEntryId] = CODE_UNREADABLE_PAYLOAD
            payload?.toPendingDto()
        }
        if (entries.isEmpty()) return SendBatchResult.Delivered(accepted = emptySet(), rejected = unreadable)

        val result = apiCall { service.synchronize(SyncPendingEntriesRequestDto(patientId = ownerUserId, entries = entries)) }
        val outcome = result.getOrNull()
        if (outcome != null) {
            val accepted = mutableSetOf<String>()
            val rejected = unreadable.toMutableMap()
            outcome.entries.forEach { item ->
                val id = operations.firstOrNull { it.clientEntryId.equals(item.clientEntryId, ignoreCase = true) }
                    ?.clientEntryId ?: return@forEach
                if (item.outcome == OUTCOME_REJECTED) {
                    rejected[id] = item.reason ?: CODE_UNEXPECTED
                } else {
                    accepted += id
                }
            }
            return SendBatchResult.Delivered(accepted = accepted, rejected = rejected)
        }
        return when (val error = result.domainErrorOrNull()) {
            // Un 400/403 de todo el lote no se arregla reintentando: quedan rechazadas con el código, a la vista.
            is DomainError.Validation -> rejectAll(operations, error.code, unreadable)
            is DomainError.Forbidden -> rejectAll(operations, error.code ?: CODE_FORBIDDEN, unreadable)
            else -> SendBatchResult.RetryLater
        }
    }

    private fun rejectAll(operations: List<PendingOperation>, code: String, unreadable: Map<String, String>) =
        SendBatchResult.Delivered(
            accepted = emptySet(),
            rejected = unreadable + operations.filterNot { it.clientEntryId in unreadable }.associate { it.clientEntryId to code },
        )

    private fun decode(payload: String): QueuedDiaryEntryPayload? =
        try {
            json.decodeFromString(QueuedDiaryEntryPayload.serializer(), payload)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val OUTCOME_REJECTED = "Rejected"
        const val CODE_UNEXPECTED = "UnexpectedError"
        const val CODE_FORBIDDEN = "PatientWriteOnly"
        const val CODE_UNREADABLE_PAYLOAD = "UnreadablePayload"
    }
}
