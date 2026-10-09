package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.iam.presentation.state.SessionExpiredUiState
import javax.inject.Inject

/** S6: estado final hasta que la persona toca «Iniciar sesión» (va a S4). Solo observa la conexión. */
@HiltViewModel
class SessionExpiredViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val state: StateFlow<SessionExpiredUiState> = connectivityObserver.isOnline
        .map { online -> SessionExpiredUiState(isOffline = !online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionExpiredUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
