package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

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
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.presentation.state.FollowUpPreparationUiState
import javax.inject.Inject

/**
 * `GET …/scheduled-follow-ups/next` (MA-3): la preparación y la modalidad de la próxima consulta.
 *
 * DECISIÓN PT25.1: si la próxima consulta ya no es la de la tarjeta de PT3 (la movieron), se muestra la que está
 * agendada ahora; sin ninguna, el aviso «Sin consulta agendada».
 */
@HiltViewModel
class FollowUpPreparationViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getNextFollowUp: GetNextFollowUpUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(FollowUpPreparationUiState())
    val state: StateFlow<FollowUpPreparationUiState> = _state.asStateFlow()

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
                    if (online && wasOffline && _state.value.loadFailed) load()
                }
            }
            load()
        }
    }

    fun onRetry() = load()

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.next == null, loadFailed = false) }
            val result = getNextFollowUp(pid)
            _state.update { state ->
                if (result.isSuccess) {
                    val next = result.getOrNull()
                    state.copy(isLoading = false, next = next, noConsultation = next == null)
                } else {
                    state.copy(
                        isLoading = false,
                        loadFailed = state.next == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
                }
            }
        }
    }
}
