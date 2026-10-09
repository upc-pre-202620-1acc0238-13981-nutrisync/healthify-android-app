package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp

/** PT25.1 · Tu consulta — cómo prepararte (+ PT25.1.V sin indicaciones). */
data class FollowUpPreparationUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val next: NextFollowUp? = null,
    /** La consulta ya no está agendada (se movió, se canceló o pasó). */
    val noConsultation: Boolean = false,
)
