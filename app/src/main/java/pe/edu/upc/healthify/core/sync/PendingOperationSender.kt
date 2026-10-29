package pe.edu.upc.healthify.core.sync

/**
 * Sends a batch of operations of one [type] to its sync endpoint (e.g.
 * `POST /diary-entries/synchronization`, `POST /self-weigh-ins/synchronization`). Each feature contributes its own
 * from `infrastructure/di` with `@Binds @IntoSet`.
 *
 * It must use `apiCall` (never throw except `CancellationException`) and always resend the stored `clientEntryId`.
 */
interface PendingOperationSender {
    /** Same as the `type` the feature queues with (e.g. `"intake.diary-entry"`). */
    val type: String

    val maxBatchSize: Int get() = DEFAULT_BATCH_SIZE

    suspend fun send(ownerUserId: Long, operations: List<PendingOperation>): SendBatchResult

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
    }
}

sealed interface SendBatchResult {
    /**
     * The backend processed the batch and reported each item (each item is reconciled separately).
     * [accepted]: created, already present or conflict resolved → removed from the queue.
     * [rejected]: `clientEntryId` → reason (`LocalTimestampCannotBeRewritten`…) → kept as rejected.
     * An item that appears in neither stays pending and is retried.
     */
    data class Delivered(
        val accepted: Set<String>,
        val rejected: Map<String, String> = emptyMap(),
    ) : SendBatchResult

    /** No network, 5xx or no session available: the whole batch is retried later with exponential backoff. */
    data object RetryLater : SendBatchResult
}
