package pe.edu.upc.healthify.core.sync

/**
 * Envía un lote de operaciones de un [type] a su endpoint de sincronización (p. ej.
 * `POST /diary-entries/synchronization`, `POST /self-weigh-ins/synchronization`). Cada feature aporta el suyo
 * desde `infrastructure/di` con `@Binds @IntoSet`.
 *
 * Debe usar `apiCall` (nunca lanzar salvo `CancellationException`) y reenviar siempre el `clientEntryId` guardado.
 */
interface PendingOperationSender {
    /** Igual al `type` con que la feature encola (p. ej. `"intake.diary-entry"`). */
    val type: String

    val maxBatchSize: Int get() = DEFAULT_BATCH_SIZE

    suspend fun send(ownerUserId: Long, operations: List<PendingOperation>): SendBatchResult

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
    }
}

sealed interface SendBatchResult {
    /**
     * El backend procesó el lote y reportó cada ítem (F20: «cada ítem se reconcilia por separado»).
     * [accepted]: creadas, ya presentes o conflicto resuelto → se borran de la cola.
     * [rejected]: `clientEntryId` → motivo (`LocalTimestampCannotBeRewritten`…) → quedan como rechazadas.
     * Un ítem que no aparezca en ninguno sigue pendiente y se reintenta.
     */
    data class Delivered(
        val accepted: Set<String>,
        val rejected: Map<String, String> = emptyMap(),
    ) : SendBatchResult

    /** Sin red, 5xx o sesión no disponible: se reintenta todo el lote más tarde con backoff exponencial. */
    data object RetryLater : SendBatchResult
}
