package pe.edu.upc.healthify.features.iam.presentation.viewmodel

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
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.application.usecase.SignInUseCase
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.presentation.state.SignInError
import pe.edu.upc.healthify.features.iam.presentation.state.SignInEvent
import pe.edu.upc.healthify.features.iam.presentation.state.SignInUiState
import javax.inject.Inject

/**
 * Single sign-in. Decides by `extensions.code`, never by the text: `InvalidCredentials` → wrong credentials,
 * `AccountLocked` → account locked (both 401). A 401 without a code is treated as wrong credentials.
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = it.error.keepLock()) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = it.error.keepLock()) }

    fun onForgotPasswordClick() {
        viewModelScope.launch { _events.send(SignInEvent.NavigateToForgotPassword) }
    }

    fun onCreateAccountClick() {
        viewModelScope.launch { _events.send(SignInEvent.NavigateToSignUp) }
    }

    fun onErrorDialogDismiss() = _state.update { it.copy(showErrorDialog = false) }

    fun onErrorDialogRetry() {
        _state.update { it.copy(showErrorDialog = false) }
        onSubmit()
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        // An email not shaped like an email or an empty password cannot sign in: same message as the backend
        // (minimal disclosure), without using up an attempt.
        val credentials = Credentials.parse(current.email, current.password)
        if (credentials == null) {
            _state.update { it.copy(error = SignInError.InvalidCredentials) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = signIn(credentials)
            val error = result.domainErrorOrNull()
            _state.update { it.copy(isSubmitting = false) }
            if (error == null) {
                _events.send(SignInEvent.NavigateToShellLoading)
            } else {
                showError(error)
            }
        }
    }

    private fun showError(error: DomainError) {
        _state.update {
            when {
                error is DomainError.Unauthorized && error.code == CODE_ACCOUNT_LOCKED ->
                    it.copy(error = SignInError.AccountLocked)
                error is DomainError.Unauthorized -> it.copy(error = SignInError.InvalidCredentials)
                else -> it.copy(showErrorDialog = true)
            }
        }
    }

    /** Editing clears the wrong credentials error; the account locked notice stays until the next attempt. */
    private fun SignInError?.keepLock(): SignInError? = takeIf { it == SignInError.AccountLocked }

    private companion object {
        const val CODE_ACCOUNT_LOCKED = "AccountLocked"
    }
}
