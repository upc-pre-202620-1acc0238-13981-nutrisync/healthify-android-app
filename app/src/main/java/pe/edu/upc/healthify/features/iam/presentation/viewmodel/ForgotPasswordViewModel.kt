package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import pe.edu.upc.healthify.features.iam.application.usecase.RequestPasswordResetUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ResetPasswordUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.VerifyPasswordResetCodeUseCase
import pe.edu.upc.healthify.features.iam.domain.entity.PasswordReset
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.Password
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetCode
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordEvent
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordStep
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordUiState
import javax.inject.Inject

/**
 * Password recovery: email → code → new password → done. Decides by `extensions.code`:
 * `PasswordResetCodeInvalid`, `WeakPassword` and `PasswordReuseNotAllowed` mark their field; `PasswordResetTokenInvalid`
 * goes back to the code step; any other error opens the retry dialog. Requesting the code always "works"
 * (the backend does not say whether the account exists), so the code step shows anyway.
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordReset: RequestPasswordResetUseCase,
    private val verifyPasswordResetCode: VerifyPasswordResetCodeUseCase,
    private val resetPassword: ResetPasswordUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events: Flow<ForgotPasswordEvent> = _events.receiveAsFlow()

    private var countdown: Job? = null

    /** The verification token. In memory only: never in the state or in `SavedStateHandle`. */
    private var resetToken: PasswordResetToken? = null

    /** The last backend call, for "Retry" in the error dialog. */
    private var retry: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, invalidEmail = false) }

    fun onCodeChange(value: String) = _state.update {
        it.copy(code = value.filter(Char::isDigit).take(PasswordResetCode.LENGTH), invalidCode = false)
    }

    fun onNewPasswordChange(value: String) =
        _state.update { it.copy(newPassword = value, weakPassword = false, passwordReused = false) }

    /** Step 1: "Send code". */
    fun onSendCode() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        val email = EmailAddress.parse(current.email)
        if (email == null) {
            _state.update { it.copy(invalidEmail = true) }
            return
        }
        sendCode(email, resent = false)
    }

    /** Step 2: "Resend code", only once the wait is over. */
    fun onResendCode() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline || current.resendInSeconds > 0) return
        val email = EmailAddress.parse(current.email) ?: return
        sendCode(email, resent = true)
    }

    /** Step 2: "Verify code". A code that is not six digits does not use up an attempt. */
    fun onVerifyCode() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        val email = EmailAddress.parse(current.email) ?: return
        val code = PasswordResetCode.parse(current.code)
        if (code == null) {
            _state.update { it.copy(invalidCode = true) }
            return
        }
        retry = ::onVerifyCode
        _state.update { it.copy(isSubmitting = true, invalidCode = false, codeResent = false, tokenExpired = false) }
        viewModelScope.launch {
            val result = verifyPasswordResetCode(email, code)
            val error = result.domainErrorOrNull()
            if (error == null) resetToken = result.getOrNull()
            _state.update {
                when {
                    error == null -> it.copy(isSubmitting = false, step = ForgotPasswordStep.NewPassword, code = "")
                    error.hasCode(CODE_RESET_CODE_INVALID) -> it.copy(isSubmitting = false, invalidCode = true)
                    else -> it.copy(isSubmitting = false, showErrorDialog = true)
                }
            }
        }
    }

    /** Step 3: "Change password". The password is checked against the sign-up rules before it is sent. */
    fun onSubmitNewPassword() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        val token = resetToken ?: return backToCode(tokenExpired = true)
        val password = Password.parse(current.newPassword)
        if (password == null) {
            _state.update { it.copy(weakPassword = true) }
            return
        }
        retry = ::onSubmitNewPassword
        _state.update { it.copy(isSubmitting = true, weakPassword = false, passwordReused = false) }
        viewModelScope.launch {
            val error = resetPassword(PasswordReset(token, password)).domainErrorOrNull()
            when {
                error == null -> {
                    resetToken = null
                    _state.update { it.copy(isSubmitting = false, step = ForgotPasswordStep.Done, newPassword = "") }
                }
                error.hasCode(CODE_RESET_TOKEN_INVALID) -> {
                    _state.update { it.copy(isSubmitting = false) }
                    backToCode(tokenExpired = true)
                }
                error.hasCode(CODE_WEAK_PASSWORD) -> _state.update { it.copy(isSubmitting = false, weakPassword = true) }
                error.hasCode(CODE_PASSWORD_REUSE) ->
                    _state.update { it.copy(isSubmitting = false, passwordReused = true) }
                else -> _state.update { it.copy(isSubmitting = false, showErrorDialog = true) }
            }
        }
    }

    /** "Use another email": goes back to step 1 keeping what was typed. */
    fun onChangeEmail() {
        resetToken = null
        _state.update {
            it.copy(
                step = ForgotPasswordStep.Email,
                code = "",
                newPassword = "",
                invalidCode = false,
                weakPassword = false,
                passwordReused = false,
                tokenExpired = false,
                codeResent = false,
            )
        }
    }

    /**
     * System "back": from the password to the code (the token is discarded and a new code is needed), from the code
     * to the email, and from the email or at the end, to sign-in.
     */
    fun onBack() {
        when (_state.value.step) {
            ForgotPasswordStep.NewPassword -> backToCode(tokenExpired = false)
            ForgotPasswordStep.Code -> onChangeEmail()
            ForgotPasswordStep.Email, ForgotPasswordStep.Done -> onGoToSignIn()
        }
    }

    fun onGoToSignIn() {
        viewModelScope.launch { _events.send(ForgotPasswordEvent.NavigateToSignIn) }
    }

    fun onErrorDialogDismiss() = _state.update { it.copy(showErrorDialog = false) }

    /** Repeats the action that failed: send, resend, verify or change the password. */
    fun onErrorDialogRetry() {
        _state.update { it.copy(showErrorDialog = false) }
        retry?.invoke()
    }

    private fun backToCode(tokenExpired: Boolean) {
        resetToken = null
        _state.update {
            it.copy(
                step = ForgotPasswordStep.Code,
                code = "",
                newPassword = "",
                weakPassword = false,
                passwordReused = false,
                tokenExpired = tokenExpired,
                codeResent = false,
            )
        }
    }

    private fun sendCode(email: EmailAddress, resent: Boolean) {
        retry = if (resent) ::onResendCode else ::onSendCode
        _state.update { it.copy(isSubmitting = true, invalidEmail = false, codeResent = false) }
        viewModelScope.launch {
            val error = requestPasswordReset(email).domainErrorOrNull()
            if (error != null) {
                _state.update { it.copy(isSubmitting = false, showErrorDialog = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    isSubmitting = false,
                    step = ForgotPasswordStep.Code,
                    codeResent = resent,
                    tokenExpired = false,
                    invalidCode = false,
                )
            }
            startResendCountdown()
        }
    }

    private fun startResendCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                _state.update { it.copy(resendInSeconds = seconds) }
                delay(ONE_SECOND_MILLIS)
            }
            _state.update { it.copy(resendInSeconds = 0) }
        }
    }

    private fun DomainError.hasCode(code: String): Boolean = this is DomainError.Validation && this.code == code

    internal companion object {
        /** Same as the backend's `Iam:PasswordResetResendSeconds`. */
        const val RESEND_COOLDOWN_SECONDS = 60
        private const val ONE_SECOND_MILLIS = 1_000L
        private const val CODE_RESET_CODE_INVALID = "PasswordResetCodeInvalid"
        private const val CODE_RESET_TOKEN_INVALID = "PasswordResetTokenInvalid"
        private const val CODE_WEAK_PASSWORD = "WeakPassword"
        private const val CODE_PASSWORD_REUSE = "PasswordReuseNotAllowed"
    }
}
