package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummary

/**
 * PT13.2 · Tu semana (+ PT13.2.V aún sin resumen). El resumen no se guarda en el teléfono: sin conexión no hay nada que
 * mostrar.
 */
data class WeeklySummaryUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val summary: WeeklySummary? = null,
    /** PT13.2.V `404 NotEnoughData`. */
    val notYet: Boolean = false,
    /** La función está apagada (`403`/`503 AiFeatureDisabled`). */
    val isOff: Boolean = false,
    val loadFailed: Boolean = false,
)
