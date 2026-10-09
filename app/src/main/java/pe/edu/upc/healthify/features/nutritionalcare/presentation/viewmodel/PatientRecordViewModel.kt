package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

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
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOwnRecordUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientRecordUiState
import javax.inject.Inject

/**
 * PT20 · el expediente propio (RM-4). Si falla toda la composición → error; si falla una sección, el backend (y el
 * mapper) la dejan vacía y el resto se muestra. Al volver a la pestaña y al volver la conexión se relee.
 */
@HiltViewModel
class PatientRecordViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getOwnRecord: GetOwnRecordUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientRecordUiState())
    val state: StateFlow<PatientRecordUiState> = _state.asStateFlow()

    private var patientId: Long? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
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
        if (_state.value.record != null) load()
    }

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.record == null, loadFailed = false) }
            val result = getOwnRecord(pid)
            _state.update { state ->
                result.getOrNull()?.let { state.copy(isLoading = false, record = it) }
                    ?: state.copy(
                        isLoading = false,
                        loadFailed = state.record == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
            }
        }
    }
}
