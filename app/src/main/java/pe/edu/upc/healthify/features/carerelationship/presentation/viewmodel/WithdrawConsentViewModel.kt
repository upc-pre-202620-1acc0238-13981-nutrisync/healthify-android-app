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
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.WithdrawConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.presentation.state.WithdrawConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.WithdrawConsentUiState
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import javax.inject.Inject

/**
 * PT23 · Retirar consentimiento (F28) con la confirmación final PT23.M. Nunca pide una razón
 * (*Consent Always Revocable · No Justification Required*).
 */
@HiltViewModel
class WithdrawConsentViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val withdrawConsent: WithdrawConsentUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(WithdrawConsentUiState())
    val state: StateFlow<WithdrawConsentUiState> = _state.asStateFlow()

    private val _events = Channel<WithdrawConsentEvent>(Channel.BUFFERED)
    val events: Flow<WithdrawConsentEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    /** «Retirar mi consentimiento» → PT23.M. */
    fun onWithdrawClick() {
        if (_state.value.isOffline) return
        _state.update { it.copy(showConfirmDialog = true) }
    }

    fun onConfirmDismiss() {
        if (_state.value.isWithdrawing) return
        _state.update { it.copy(showConfirmDialog = false) }
    }

    /** «Sí, retirar». */
    fun onConfirmWithdraw() {
        val current = _state.value
        if (current.isWithdrawing || current.isOffline) return
        _state.update { it.copy(isWithdrawing = true, showServerError = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _state.update { it.copy(isWithdrawing = false, showConfirmDialog = false) }
                return@launch
            }
            val error = withdrawConsent(user.id.value).domainErrorOrNull()
            _state.update { it.copy(isWithdrawing = false, showConfirmDialog = false) }
            when {
                error == null -> _events.send(WithdrawConsentEvent.NavigateToWithdrawn)
                error is DomainError.Forbidden && error.code == WithdrawConsentUseCase.CODE_NO_ACTIVE_CONSENT ->
                    _state.update { it.copy(showNoActiveConsent = true) }
                else -> _state.update { it.copy(showServerError = true) }
            }
        }
    }

    /** «Tu consentimiento ya no está vigente.» → PT24 igual (nota de PT23). */
    fun onNoActiveConsentAcknowledged() {
        _state.update { it.copy(showNoActiveConsent = false) }
        viewModelScope.launch { _events.send(WithdrawConsentEvent.NavigateToWithdrawn) }
    }

    fun onServerErrorRetry() {
        _state.update { it.copy(showServerError = false) }
        onConfirmWithdraw()
    }

    fun onServerErrorDismiss() = _state.update { it.copy(showServerError = false) }
}
