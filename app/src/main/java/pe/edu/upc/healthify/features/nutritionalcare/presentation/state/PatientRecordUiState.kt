package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord

/**
 * My record (plus the loading and empty section states). Not available offline: the record is not stored on
 * the phone.
 */
data class PatientRecordUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val record: PatientOwnRecord? = null,
)
