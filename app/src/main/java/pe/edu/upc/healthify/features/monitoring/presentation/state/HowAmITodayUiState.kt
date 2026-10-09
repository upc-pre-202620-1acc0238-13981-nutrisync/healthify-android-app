package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

/**
 * PT15 · Cómo voy hoy. El indicador vive en Monitoring y no está disponible sin conexión (PT15.O).
 *
 * @param outcome `null` mientras carga, sin conexión o con error.
 */
data class HowAmITodayUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val outcome: ComplianceOutcome? = null,
    val loadFailed: Boolean = false,
)
