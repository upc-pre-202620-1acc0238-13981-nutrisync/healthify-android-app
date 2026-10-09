package pe.edu.upc.healthify.features.main.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.iam.application.usecase.SignOutUseCase
import javax.inject.Inject

/** «Cerrar sesión» de los shells: el estado indica si está en curso; al terminar, el grafo raíz limpia el back stack. */
data class SignOutUiState(val isSigningOut: Boolean = false)

sealed interface SignOutEvent {
    data object SignedOut : SignOutEvent
}

@HiltViewModel
class SignOutViewModel @Inject constructor(
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignOutUiState())
    val state: StateFlow<SignOutUiState> = _state.asStateFlow()

    private val _events = Channel<SignOutEvent>(Channel.BUFFERED)
    val events: Flow<SignOutEvent> = _events.receiveAsFlow()

    fun onSignOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            signOut()
            _state.update { it.copy(isSigningOut = false) }
            _events.send(SignOutEvent.SignedOut)
        }
    }
}
