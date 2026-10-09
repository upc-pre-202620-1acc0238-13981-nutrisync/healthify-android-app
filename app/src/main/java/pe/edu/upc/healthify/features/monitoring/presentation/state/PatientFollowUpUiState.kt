package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel

/**
 * PAC-2 · Seguimiento del paciente (solo lectura). [aiSummary] es el texto de IA-5 o `null` (la tarjeta no se muestra).
 * No se guarda en el teléfono.
 */
data class PatientFollowUpUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val panel: PatientMonitoringPanel? = null,
    val aiSummary: String? = null,
)
