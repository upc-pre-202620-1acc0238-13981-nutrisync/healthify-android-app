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
 * IAM-2 · Recuperar contraseña: correo → código → contraseña nueva → listo. Decide por `extensions.code` (X-3):
 * `PasswordResetCodeInvalid`, `WeakPassword` y `PasswordReuseNotAllowed` marcan su campo; `PasswordResetTokenInvalid`
 * vuelve al paso del código; cualquier otro error abre el diálogo de reintento. Pedir el código siempre «funciona»
 * (el backend no dice si la cuenta existe), así que el paso del código se muestra igual.
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

    /** El token de la verificación. Solo en memoria: nunca en el estado ni en `SavedStateHandle`. */
    private var resetToken: PasswordResetToken? = null

    /** La última llamada al backend, para «Reintentar» del diálogo de error. */
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

    /** Paso 1: «Enviar código». */
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

    /** Paso 2: «Reenviar código», solo cuando terminó la espera. */
    fun onResendCode() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline || current.resendInSeconds > 0) return
        val email = EmailAddress.parse(current.email) ?: return
        sendCode(email, resent = true)
    }

    /** Paso 2: «Verificar código». Un código que no son seis dígitos no gasta un intento. */
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

    /** Paso 3: «Cambiar contraseña». La contraseña se valida con las reglas de registro antes de enviarla. */
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

    /** «Usar otro correo»: vuelve al paso 1 conservando lo escrito. */
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
     * «Atrás» del sistema: de la contraseña al código (el token se descarta y hará falta un código nuevo), del código
     * al correo, y desde el correo o al terminar, a S4.
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

    /** Repite la acción que falló: enviar, reenviar, verificar o cambiar la contraseña. */
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
        /** Igual que `Iam:PasswordResetResendSeconds` del backend. */
        const val RESEND_COOLDOWN_SECONDS = 60
        private const val ONE_SECOND_MILLIS = 1_000L
        private const val CODE_RESET_CODE_INVALID = "PasswordResetCodeInvalid"
        private const val CODE_RESET_TOKEN_INVALID = "PasswordResetTokenInvalid"
        private const val CODE_WEAK_PASSWORD = "WeakPassword"
        private const val CODE_PASSWORD_REUSE = "PasswordReuseNotAllowed"
    }
}
