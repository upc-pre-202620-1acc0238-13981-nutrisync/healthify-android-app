package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GrantConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentErrorAction
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentUiState
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import javax.inject.Inject

/**
 * PT2 · Consentimiento y alcance (F6). «Doy mi consentimiento» activa el vínculo y lleva a PT3; «Ahora no» deja el
 * vínculo pendiente (PT2.1). La IA se acepta aparte con el interruptor (CR-2).
 */
@HiltViewModel
class ConsentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val grantConsent: GrantConsentUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val careLinkId: Long = checkNotNull(savedStateHandle[ConsentRoute.ARG_CARE_LINK_ID]) {
        "ConsentRoute needs a careLinkId"
    }

    private val _state = MutableStateFlow(ConsentUiState())
    val state: StateFlow<ConsentUiState> = _state.asStateFlow()

    private val _events = Channel<ConsentEvent>(Channel.BUFFERED)
    val events: Flow<ConsentEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onAiProcessingChange(granted: Boolean) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(aiProcessingGranted = granted) }
    }

    fun onGrantConsent() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        _state.update { it.copy(isSubmitting = true, showServerError = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _state.update { it.copy(isSubmitting = false) }
                return@launch
            }
            val result = grantConsent(user.id.value, careLinkId, current.aiProcessingGranted)
            val error = result.domainErrorOrNull()
            _state.update { it.copy(isSubmitting = false) }
            if (error == null) {
                _events.send(ConsentEvent.NavigateToHome)
            } else {
                val consentError = error.toConsentError()
                _state.update {
                    if (consentError == null) it.copy(showServerError = true) else it.copy(error = consentError)
                }
            }
        }
    }

    fun onNotNow() {
        if (_state.value.isSubmitting) return
        viewModelScope.launch { _events.send(ConsentEvent.NavigateToPendingConsent) }
    }

    /** Botón del aviso de error: cada caso lleva a su sitio (el vínculo ya activo → PT3; cerrado → PT1). */
    fun onErrorAction() {
        val error = _state.value.error ?: return
        _state.update { it.copy(error = null) }
        val event = when (error.action) {
            ConsentErrorAction.Dismiss -> return
            ConsentErrorAction.GoHome -> ConsentEvent.NavigateToHome
            ConsentErrorAction.ScanInvitation -> ConsentEvent.NavigateToScanInvitation
        }
        viewModelScope.launch { _events.send(event) }
    }

    /** Cerrar el aviso con atrás o tocando fuera equivale a su botón: el estado del vínculo ya cambió. */
    fun onErrorDismiss() = onErrorAction()

    fun onServerErrorRetry() {
        _state.update { it.copy(showServerError = false) }
        onGrantConsent()
    }

    fun onServerErrorDismiss() = _state.update { it.copy(showServerError = false) }
}

/** Validaciones de la nota de PT2; `null` = «error servidor» (404 del vínculo, red, 5xx…). */
internal fun DomainError.toConsentError(): ConsentError? = when {
    this is DomainError.Validation && code == "ConsentScopeRequired" -> ConsentError.ScopeRequired
    this is DomainError.Conflict -> when (code) {
        "DischargedLinkCannotBeReactivated" -> ConsentError.LinkDischarged
        "CareLinkAlreadyRevoked" -> ConsentError.LinkRevoked
        "ConsentAlreadyGranted" -> ConsentError.AlreadyGranted
        else -> null
    }
    else -> null
}
