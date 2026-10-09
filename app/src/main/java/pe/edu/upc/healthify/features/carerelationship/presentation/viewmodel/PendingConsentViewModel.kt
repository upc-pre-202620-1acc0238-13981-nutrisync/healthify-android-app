package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPendingCareLinkUseCase
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PendingConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PendingConsentUiState
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.SignOutUseCase
import javax.inject.Inject

/** PT2.1 · Vínculo pendiente: «Revisar y dar mi consentimiento» (→ PT2) o «Cerrar sesión». */
@HiltViewModel
class PendingConsentViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getPendingCareLink: GetPendingCareLinkUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PendingConsentUiState())
    val state: StateFlow<PendingConsentUiState> = _state.asStateFlow()

    private val _events = Channel<PendingConsentEvent>(Channel.BUFFERED)
    val events: Flow<PendingConsentEvent> = _events.receiveAsFlow()

    fun onReviewConsent() {
        if (_state.value.isSigningOut) return
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val careLinkId = getPendingCareLink(user.id.value)
            _events.send(
                if (careLinkId != null) {
                    PendingConsentEvent.NavigateToConsent(careLinkId.value)
                } else {
                    PendingConsentEvent.NavigateToScanInvitation
                },
            )
        }
    }

    fun onSignOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            signOut()
            _state.update { it.copy(isSigningOut = false) }
            _events.send(PendingConsentEvent.SignedOut)
        }
    }
}
