package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PatientRosterUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.RosterItem
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import javax.inject.Inject

/**
 * PR1 · Mi cartera (+ PR1.L, PR1.V, PR1.O). La cartera no se guarda en el teléfono: sin conexión y sin una lectura
 * previa en memoria se muestra PR1.O. Se relee al volver a la pestaña y al volver la conexión.
 */
@HiltViewModel
class PatientRosterViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getPatientRoster: GetPatientRosterUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientRosterUiState())
    val state: StateFlow<PatientRosterUiState> = _state.asStateFlow()

    private var practitionerId: Long? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            practitionerId = user.id.value
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) load()
                }
            }
            load()
        }
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.patients != null) load()
    }

    private fun load() {
        val id = practitionerId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.patients == null, loadFailed = false) }
            val result = getPatientRoster(id)
            _state.update { state ->
                result.getOrNull()?.let { roster ->
                    state.copy(isLoading = false, patients = roster.patients.map(RosterItem::of))
                } ?: state.copy(
                    isLoading = false,
                    loadFailed = state.patients == null,
                    isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }
}
