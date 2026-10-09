package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import java.time.Instant
import java.time.LocalDate

/**
 * PT14 · Diario (+ PT14.V vacío, PT14.O sin conexión, PT14.L cargando).
 *
 * @param items lo que se ve del día elegido, ya ordenado (fotos por analizar, «Por confirmar», pendientes de enviar y
 *   el resto de la más reciente a la más antigua).
 * @param cachedAt el día viene de la copia del teléfono (sin conexión).
 * @param pendingCount comidas en la cola que todavía no se enviaron (de cualquier día): «Ver pendientes (2)».
 * @param showIdeasCard la card «¿No sabes qué comer?»: solo hoy y con la preferencia de ideas encendida (IA-3).
 * @param earliestDate día en que se creó la cuenta: el primer día del diario.
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

    /** No se retrocede antes del día en que se creó la cuenta (sin esa fecha todavía, sin límite). */
    val canGoToPreviousDay: Boolean get() = earliestDate == null || selectedDate.isAfter(earliestDate)
    val isToday: Boolean get() = selectedDate == today

    /** PT14.V: el día se leyó (o hay pendientes) y no tiene nada. */
    val isEmpty: Boolean get() = !isLoading && hasDay && items.isEmpty()

    /** Sin conexión y sin copia de ese día ni pendientes: «Este día no está guardado en tu teléfono». */
    val isOfflineWithoutCopy: Boolean get() = !isLoading && !hasDay && isOffline && items.isEmpty()
}

/** Una fila del diario. [key] es estable para `LazyColumn`. */
sealed interface DiaryItem {
    val key: String

    data class Entry(val entry: DiaryEntry) : DiaryItem {
        override val key: String get() = "entry-${entry.id.value}"
    }

    /** Entradas registradas juntas desde una idea (IN-6): una sola card. */
    data class MealGroup(val groupId: String, val entries: List<DiaryEntry>) : DiaryItem {
        override val key: String get() = "group-$groupId"
    }

    /** En la cola del teléfono: «Pendiente de enviar» (o «No se envió» si el backend la rechazó). */
    data class Pending(val entry: PendingDiaryEntry) : DiaryItem {
        override val key: String get() = "pending-${entry.clientEntryId.value}"
    }

    /** Foto tomada sin conexión que espera para analizarse. */
    data class PendingPhoto(val photo: PendingMealPhoto) : DiaryItem {
        override val key: String get() = "photo-${photo.id}"
    }
}

sealed interface DiaryEvent {
    data class ShowLoggedMeal(val notice: LoggedMealNotice) : DiaryEvent
}
