package pe.edu.upc.healthify.core.sync

import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity

/** Lo que ve una feature de una operación encolada (nunca la entity de Room). */
data class PendingOperation(
    val clientEntryId: String,
    val type: String,
    val ownerUserId: Long,
    val payload: String,
    val localTimestamp: String,
    val attempts: Int,
    val status: Status,
    val rejectionCode: String?,
) {
    enum class Status { PENDING, REJECTED }
}

internal fun PendingOperationEntity.toModel(): PendingOperation = PendingOperation(
    clientEntryId = clientEntryId,
    type = type,
    ownerUserId = ownerUserId,
    payload = payload,
    localTimestamp = localTimestamp,
    attempts = attempts,
    status = if (status == PendingOperationEntity.STATUS_REJECTED) {
        PendingOperation.Status.REJECTED
    } else {
        PendingOperation.Status.PENDING
    },
    rejectionCode = rejectionCode,
)
