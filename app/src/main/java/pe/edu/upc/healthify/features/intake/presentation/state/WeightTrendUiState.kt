package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import java.time.Instant

/**
 * PT13 · Tendencia de peso (+ PT13.1 Snackbar, PT13.V, PT13.L). Nunca lleva un «peso de hoy»: solo la serie suavizada.
 *
 * @param trend la tendencia con al menos 2 puntos en el rango (si no, [hasNoTrend]).
 * @param weeklyCard la card «Tu semana» (IA-2); `null` = no aparece (apagada, sin conexión o con error).
 */
data class WeightTrendUiState(
    val isLoading: Boolean = true,
    val trend: WeightTrend? = null,
    val hasNoTrend: Boolean = false,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val weeklyCard: WeeklyCard? = null,
    val weeks: Int,
) {
    /** PT13.V (o `404 WeightTrendNotFound`, mismo copy). */
    val isEmpty: Boolean get() = !isLoading && hasNoTrend

    /** Sin conexión y sin copia en el teléfono: no hay nada que dibujar. */
    val isOfflineWithoutCopy: Boolean get() = !isLoading && isOffline && trend == null && !hasNoTrend && loadFailed

    /** «Algunos registros no siguieron el protocolo…» (IN-5). */
    val showExcluded: Boolean get() = (trend?.excludedReadingsCount ?: 0) > 0

    val cachedAt: Instant? get() = trend?.takeIf { it.fromCache }?.savedAt
}

/** La card «Tu semana» de PT13, ya decidida (el titular de la IA va tal cual). */
sealed interface WeeklyCard {
    data class Ready(val headline: String) : WeeklyCard

    /** PT13.2.V: todavía no hay resumen; la card invita y abre el estado vacío. */
    data object NotYet : WeeklyCard
}

sealed interface WeightTrendEvent {
    /** PT13.1 «Autopesaje guardado. No se muestra el peso del día.» (o «Se enviará…» sin conexión). */
    data class ShowSaved(val outcome: SelfWeighInOutcome) : WeightTrendEvent
}
