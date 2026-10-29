package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import java.time.Instant
import java.time.LocalDate

/**
 * Diary (with its empty, offline and loading states).
 *
 * @param items what is shown for the selected day, already sorted (photos to analyze, "To confirm", pending to send
 *   and the rest from newest to oldest).
 * @param cachedAt the day comes from the phone's copy (offline).
 * @param pendingCount queued meals not sent yet (from any day): "See pending (2)".
 * @param showIdeasCard the "Not sure what to eat?" card: only today and with the ideas preference on.
 * @param earliestDate the day the account was created: the diary's first day.
 */
data class DiaryUiState(
    val today: LocalDate,
    val selectedDate: LocalDate = today,
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val items: List<DiaryItem> = emptyList(),
    val hasDay: Boolean = false,
    val cachedAt: Instant? = null,
    val loadFailed: Boolean = false,
    val pendingCount: Int = 0,
    val showIdeasCard: Boolean = false,
    val earliestDate: LocalDate? = null,
) {
    val canGoToNextDay: Boolean get() = selectedDate.isBefore(today)

    /** It does not go back before the day the account was created (with no such date yet, no limit). */
    val canGoToPreviousDay: Boolean get() = earliestDate == null || selectedDate.isAfter(earliestDate)
    val isToday: Boolean get() = selectedDate == today

    /** Empty state: the day was read (or there are pending items) and it has nothing. */
    val isEmpty: Boolean get() = !isLoading && hasDay && items.isEmpty()

    /** Offline with no copy of that day and no pending items: "This day is not saved on your phone". */
    val isOfflineWithoutCopy: Boolean get() = !isLoading && !hasDay && isOffline && items.isEmpty()
}

/** One diary row. [key] is stable for `LazyColumn`. */
sealed interface DiaryItem {
    val key: String

    data class Entry(val entry: DiaryEntry) : DiaryItem {
        override val key: String get() = "entry-${entry.id.value}"
    }

    /** Entries logged together from an idea: a single card. */
    data class MealGroup(val groupId: String, val entries: List<DiaryEntry>) : DiaryItem {
        override val key: String get() = "group-$groupId"
    }

    /** In the phone's queue: "Pending to send" (or "Not sent" if the backend rejected it). */
    data class Pending(val entry: PendingDiaryEntry) : DiaryItem {
        override val key: String get() = "pending-${entry.clientEntryId.value}"
    }

    /** A photo taken offline that waits to be analyzed. */
    data class PendingPhoto(val photo: PendingMealPhoto) : DiaryItem {
        override val key: String get() = "photo-${photo.id}"
    }
}

sealed interface DiaryEvent {
    data class ShowLoggedMeal(val notice: LoggedMealNotice) : DiaryEvent
}
