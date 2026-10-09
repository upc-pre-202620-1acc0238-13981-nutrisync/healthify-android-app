package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetTodayProgressUseCase
import pe.edu.upc.healthify.features.monitoring.presentation.state.HowAmITodayUiState
import javax.inject.Inject

/** PT15 · Cómo voy hoy: el resultado del día del teléfono (Met / Exceeded / Short / Unlogged). */
@HiltViewModel
class HowAmITodayViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getTodayProgress: GetTodayProgressUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(HowAmITodayUiState())
    val state: StateFlow<HowAmITodayUiState> = _state.asStateFlow()

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

    private fun load() {
        _state.update { it.copy(isLoading = it.outcome == null, loadFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val result = getTodayProgress(user.id.value)
            val error = result.domainErrorOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    outcome = result.getOrNull()?.outcome ?: it.outcome,
                    isOffline = it.isOffline || error == DomainError.Network,
                    loadFailed = error != null && error != DomainError.Network,
                )
            }
        }
    }
}
