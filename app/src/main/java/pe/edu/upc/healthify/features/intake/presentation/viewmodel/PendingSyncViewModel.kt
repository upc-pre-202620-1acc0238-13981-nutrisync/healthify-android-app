package pe.edu.upc.healthify.features.intake.presentation.viewmodel

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
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingSelfWeighInsUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.RequestDiarySyncUseCase
import pe.edu.upc.healthify.features.intake.presentation.state.PendingSyncUiState
import javax.inject.Inject

/**
 * PT19 · Pendientes por sincronizar: la cola offline del teléfono. Se envía sola con red (WorkManager); abrir la
 * pantalla solo adelanta el envío. Lo enviado sale de la lista; lo rechazado queda con su motivo y la hora tal como
 * se guardó (*Local Timestamp Never Rewritten*).
 */
@HiltViewModel
class PendingSyncViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observePendingEntries: ObservePendingDiaryEntriesUseCase,
    private val observePendingSelfWeighIns: ObservePendingSelfWeighInsUseCase,
    private val requestDiarySync: RequestDiarySyncUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PendingSyncUiState())
    val state: StateFlow<PendingSyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) requestDiarySync()
            }
        }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            // Una sola cola en el teléfono: el mismo envío lleva comidas y autopesajes.
            requestDiarySync()
            launch {
                observePendingSelfWeighIns(user.id.value).collect { weighIns ->
                    _state.update { it.copy(weighIns = weighIns) }
                }
            }
            observePendingEntries(user.id.value).collect { entries ->
                _state.update { it.copy(isLoading = false, entries = entries) }
            }
        }
    }
}
