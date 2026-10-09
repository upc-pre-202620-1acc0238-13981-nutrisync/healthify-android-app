package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

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
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanVersionsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionItem
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionsUiState
import javax.inject.Inject

/** PT4.1 · Versiones del plan (`GET /patients/{pid}/plan-versions`, NC-8). */
@HiltViewModel
class PlanVersionsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getPlanVersions: GetPlanVersionsUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanVersionsUiState())
    val state: StateFlow<PlanVersionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline && _state.value.versions.isEmpty()) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _state.update { it.copy(isLoading = it.versions.isEmpty(), loadFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val result = getPlanVersions(user.id.value)
            val versions = result.getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    versions = versions?.map { v -> PlanVersionItem(v.version, v.publishedAt, v.energyKcal, v.isActive) }
                        ?: it.versions,
                    // Sin red se muestra el estado «Sin conexión», no un error.
                    loadFailed = versions == null && result.domainErrorOrNull() != DomainError.Network,
                    isOffline = it.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }
}
