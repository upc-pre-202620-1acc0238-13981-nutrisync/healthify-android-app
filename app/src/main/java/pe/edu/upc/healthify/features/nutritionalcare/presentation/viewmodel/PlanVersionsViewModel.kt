package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.isSecondary
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.toUiText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PlanVersionsRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanChangeLine
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionItem
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionsUiState
import javax.inject.Inject

/**
 * Plan versions (`GET /patients/{pid}/plan-versions`). From a past consultation it arrives with
 * the version whose detail opens on load.
 */
@HiltViewModel
class PlanVersionsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getPlanVersions: GetPlanVersionsUseCase,
    connectivityObserver: ConnectivityObserver,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        PlanVersionsUiState(selectedVersion = savedStateHandle.get<Int>(PlanVersionsRoute.ARG_VERSION)),
    )
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

    fun onVersionSelected(version: Int) = _state.update { it.copy(selectedVersion = version) }

    fun onDetailDismissed() = _state.update { it.copy(selectedVersion = null) }

    private fun load() {
        _state.update { it.copy(isLoading = it.versions.isEmpty(), loadFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val result = getPlanVersions(user.id.value)
            val versions = result.getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    versions = versions?.map(PlanVersion::toItem) ?: it.versions,
                    // Offline shows the "No connection" state, not an error.
                    loadFailed = versions == null && result.domainErrorOrNull() != DomainError.Network,
                    isOffline = it.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }
}

private fun PlanVersion.toItem() = PlanVersionItem(
    version = version,
    publishedAt = publishedAt,
    energyKcal = energyKcal,
    isActive = isActive,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG,
    guidelines = guidelines.map { it.toUiText() },
    restrictions = restrictions.map { it.toUiText() },
    changes = changesFromPrevious.map { PlanChangeLine(it.toUiText(), secondary = it.isSecondary) },
    patientMessage = patientMessage,
)
