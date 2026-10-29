package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

/**
 * How am I doing today. The indicator lives in Monitoring and is not available offline.
 *
 * @param outcome `null` while loading, offline or on error.
 */
data class HowAmITodayUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val outcome: ComplianceOutcome? = null,
    val loadFailed: Boolean = false,
)
