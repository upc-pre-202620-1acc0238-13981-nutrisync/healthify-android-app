package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp

/** Your visit - how to prepare (with the no-instructions state). */
data class FollowUpPreparationUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val next: NextFollowUp? = null,
    /** The visit is no longer scheduled (moved, cancelled or past). */
    val noConsultation: Boolean = false,
)
