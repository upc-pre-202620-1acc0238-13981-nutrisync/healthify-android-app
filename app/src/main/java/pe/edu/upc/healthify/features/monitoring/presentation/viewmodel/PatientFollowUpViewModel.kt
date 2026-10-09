package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetMonitoringPanelUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetMonitoringSummaryUseCase
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientFollowUpUiState
import javax.inject.Inject

/**
 * PAC-2 · Seguimiento (RM-3) con el resumen con IA (IA-5) pedido en paralelo. Solo lectura. Si el resumen no llega
 * (sin consentimiento de IA del paciente, función apagada, cuota, error), la tarjeta no se muestra y el panel sigue.
 */
@HiltViewModel
class PatientFollowUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMonitoringPanel: GetMonitoringPanelUseCase,
    private val getMonitoringSummary: GetMonitoringSummaryUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[ARG_PATIENT_ID])

    private val _state = MutableStateFlow(PatientFollowUpUiState())
    val state: StateFlow<PatientFollowUpUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.panel != null) load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.panel == null, loadFailed = false) }
            val summary = async { getMonitoringSummary(patientId) }
            val result = getMonitoringPanel(patientId)
            val summaryText = summary.await().getOrNull()?.text
            _state.update { state ->
                result.getOrNull()?.let { state.copy(isLoading = false, panel = it, aiSummary = summaryText) }
                    ?: state.copy(
                        isLoading = false,
                        loadFailed = state.panel == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
            }
        }
    }

    companion object {
        /** Argumento de la ficha del paciente (`PractitionerPatientRoute.patientId`, por nombre de propiedad). */
        const val ARG_PATIENT_ID = "patientId"
    }
}
