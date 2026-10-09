package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import java.time.Instant

/** PT19 · Pendientes (+ PT19.V «Todo al día»): comidas y autopesajes de la cola del teléfono. */
data class PendingSyncUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val entries: List<PendingDiaryEntry> = emptyList(),
    val weighIns: List<PendingSelfWeighIn> = emptyList(),
) {
    /** Todo lo pendiente, por la hora en que se registró (la que declaró el paciente). */
    val items: List<PendingSyncItem>
        get() = (entries.map(PendingSyncItem::Meal) + weighIns.map(PendingSyncItem::WeighIn)).sortedBy { it.registeredAt }

    /** «Sincronizando…»: hay red y algo esperando (el worker lo está enviando). */
    val isSyncing: Boolean get() = !isOffline && items.any { !it.isRejected }

    val isEmpty: Boolean get() = !isLoading && entries.isEmpty() && weighIns.isEmpty()
}

/** Una fila de PT19. */
sealed interface PendingSyncItem {
    val key: String
    val registeredAt: Instant
    val isRejected: Boolean

    data class Meal(val entry: PendingDiaryEntry) : PendingSyncItem {
        override val key: String get() = entry.clientEntryId.value
        override val registeredAt: Instant get() = entry.localTimestamp.instant
        override val isRejected: Boolean get() = entry.isRejected
    }

    data class WeighIn(val weighIn: PendingSelfWeighIn) : PendingSyncItem {
        override val key: String get() = weighIn.clientEntryId.value
        override val registeredAt: Instant get() = weighIn.localTimestamp.instant
        override val isRejected: Boolean get() = weighIn.isRejected
    }
}
