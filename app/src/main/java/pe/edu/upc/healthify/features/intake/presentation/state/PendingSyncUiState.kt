package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import java.time.Instant

/** Pending items (and the "All caught up" state): meals and self weigh-ins in the phone's queue. */
data class PendingSyncUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val entries: List<PendingDiaryEntry> = emptyList(),
    val weighIns: List<PendingSelfWeighIn> = emptyList(),
) {
    /** Everything pending, by the time it was logged (the one the patient declared). */
    val items: List<PendingSyncItem>
        get() = (entries.map(PendingSyncItem::Meal) + weighIns.map(PendingSyncItem::WeighIn)).sortedBy { it.registeredAt }

    /** "Syncing…": there is network and something waiting (the worker is sending it). */
    val isSyncing: Boolean get() = !isOffline && items.any { !it.isRejected }

    val isEmpty: Boolean get() = !isLoading && entries.isEmpty() && weighIns.isEmpty()
}

/** One row of the pending list. */
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
