package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord

/**
 * PT20 · Mi expediente (+ PT20.L cargando y sección vacía). No disponible sin conexión: el expediente no se guarda en
 * el teléfono.
 */
data class PatientRecordUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val record: PatientOwnRecord? = null,
)
