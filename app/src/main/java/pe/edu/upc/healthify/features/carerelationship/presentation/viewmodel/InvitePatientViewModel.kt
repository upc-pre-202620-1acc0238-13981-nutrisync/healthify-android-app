package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.IssueInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InviteError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InvitePatientUiState
import java.time.Clock
import javax.inject.Inject

/**
 * Generate invitation. On entry it issues a code with the default validity (24 h, as in the design); the
 * countdown is recomputed every second with the phone's clock. "Generate another code" issues a new one: the previous
 * one stays valid until it expires.
 *
 * Changing "Expires in" issues a new code with that validity (the one already shown cannot change).
 */
@HiltViewModel
class InvitePatientViewModel @Inject constructor(
    private val issueInvitation: IssueInvitationUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(InvitePatientUiState())
    val state: StateFlow<InvitePatientUiState> = _state.asStateFlow()

    private var invitation: IssuedInvitation? = null
    private var issueJob: Job? = null
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            // Offline on entry, no attempt is made: the error from the design notes is shown.
            if (connectivityObserver.isOnline.first()) {
                generate()
            } else {
                _state.update { it.copy(isGenerating = false, error = InviteError.OFFLINE) }
            }
        }
    }

    /** "Generate another code" and retry after an error. */
    fun onGenerateAnother() = generate()

    fun onValidityChange(validity: InvitationValidity) {
        if (validity == _state.value.validity && invitation != null) return
        _state.update { it.copy(validity = validity) }
        generate()
    }

    private fun generate() {
        if (issueJob?.isActive == true) return
        issueJob = viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }
            val result = issueInvitation(_state.value.validity)
            val issued = result.getOrNull()
            if (issued != null) {
                invitation = issued
                _state.update {
                    it.copy(isGenerating = false, token = issued.token, isExpired = false, error = null)
                }
                startCountdown(issued)
            } else {
                val error = when (val domainError = result.domainErrorOrNull()) {
                    DomainError.Network -> InviteError.OFFLINE
                    is DomainError.Validation ->
                        if (domainError.code == EXPIRATION_DATE_REQUIRED) InviteError.EXPIRATION_REQUIRED else InviteError.GENERIC
                    else -> InviteError.GENERIC
                }
                _state.update { it.copy(isGenerating = false, error = error) }
            }
        }
    }

    private fun startCountdown(issued: IssuedInvitation) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val now = clock.instant()
                val expired = issued.isExpired(now)
                _state.update { it.copy(remaining = issued.remaining(now), isExpired = expired) }
                if (expired) break
                delay(TICK_MILLIS)
            }
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val EXPIRATION_DATE_REQUIRED = "ExpirationDateRequired"
    }
}
