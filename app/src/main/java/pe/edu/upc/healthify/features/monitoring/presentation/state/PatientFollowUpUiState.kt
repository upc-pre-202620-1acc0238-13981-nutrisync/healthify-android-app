package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel

/**
 * Patient follow-up (read-only). [aiSummary] is the AI summary text or `null` (the card is hidden).
 * Not stored on the phone.
 */
data class PatientFollowUpUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val panel: PatientMonitoringPanel? = null,
    val aiSummary: String? = null,
)
