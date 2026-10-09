package pe.edu.upc.healthify.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.onboarding.presentation.state.WelcomeEvent
import pe.edu.upc.healthify.features.onboarding.presentation.state.WelcomeUiState
import javax.inject.Inject

/** S2: los dos botones llevan a S3/S4, que necesitan red; sin conexión se muestra el banner y no se navega. */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    private val _events = Channel<WelcomeEvent>(Channel.BUFFERED)
    val events: Flow<WelcomeEvent> = _events.receiveAsFlow()

    private val isOnline: StateFlow<Boolean> =
        connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            isOnline.collect { online -> if (online) _state.update { it.copy(showOfflineBanner = false) } }
        }
    }

    fun onCreateAccount() = continueTo(WelcomeEvent.NavigateToSignUp)

    fun onHaveAccount() = continueTo(WelcomeEvent.NavigateToSignIn)

    private fun continueTo(event: WelcomeEvent) {
        if (!isOnline.value) {
            _state.update { it.copy(showOfflineBanner = true) }
            return
        }
        viewModelScope.launch { _events.send(event) }
    }
}
