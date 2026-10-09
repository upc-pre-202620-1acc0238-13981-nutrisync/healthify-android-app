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
import pe.edu.upc.healthify.features.iam.application.usecase.RegisterResult
import pe.edu.upc.healthify.features.iam.application.usecase.RegisterUseCase
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpProblem
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpValidation
import pe.edu.upc.healthify.features.iam.domain.entity.validateNewAccount
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEmailError
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEvent
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpFieldErrors
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpUiState
import javax.inject.Inject

/**
 * S3 · Registro. Valida con los value objects (mismas reglas que el backend) antes de enviar y traduce los
 * códigos del backend (`InvalidEmail`, `WeakPassword`, `NameRequired`, `RoleNotDeclared`, `InvalidRole`,
 * `EmailAlreadyTaken`) a los errores de cada campo de S3.E.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val register: RegisterUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events: Flow<SignUpEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onGivenNamesChange(value: String) =
        _state.update { it.copy(givenNames = value, errors = it.errors.copy(givenNames = null)) }

    fun onFamilyNamesChange(value: String) =
        _state.update { it.copy(familyNames = value, errors = it.errors.copy(familyNames = null)) }

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, errors = it.errors.copy(email = null)) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, errors = it.errors.copy(weakPassword = false)) }

    fun onRoleSelect(role: UserRole) =
        _state.update { it.copy(role = role, errors = it.errors.copy(roleMissing = false)) }

    fun onSignInClick() {
        viewModelScope.launch { _events.send(SignUpEvent.NavigateToSignIn) }
    }

    fun onErrorDialogDismiss() = _state.update { it.copy(showErrorDialog = false) }

    fun onErrorDialogRetry() {
        _state.update { it.copy(showErrorDialog = false) }
        onSubmit()
    }

    fun onSubmit() {
        val current = _state.value
        if (current.isSubmitting || current.isOffline) return
        val validation = validateNewAccount(
            givenNames = current.givenNames,
            familyNames = current.familyNames,
            email = current.email,
            password = current.password,
            role = current.role,
        )
        when (validation) {
            is SignUpValidation.Invalid -> _state.update { it.copy(errors = validation.problems.toFieldErrors()) }
            is SignUpValidation.Valid -> {
                _state.update { it.copy(isSubmitting = true, errors = SignUpFieldErrors()) }
                viewModelScope.launch {
                    val result = register(validation.account)
                    _state.update { it.copy(isSubmitting = false) }
                    onRegisterResult(result)
                }
            }
        }
    }

    private suspend fun onRegisterResult(result: RegisterResult) {
        when (result) {
            is RegisterResult.SignedIn -> _events.send(SignUpEvent.NavigateToShellLoading)
            // DECISIÓN S3: la cuenta ya existe; volver a «Crear cuenta» respondería EmailAlreadyTaken. Se sigue en S4.
            is RegisterResult.CreatedWithoutSession -> _events.send(SignUpEvent.NavigateToSignIn)
            is RegisterResult.Failed -> showError(result.error)
        }
    }

    private fun showError(error: DomainError) {
        val fieldErrors = error.toFieldErrors()
        _state.update {
            when {
                fieldErrors != null -> it.copy(errors = fieldErrors)
                else -> it.copy(showErrorDialog = true)
            }
        }
    }

    private fun DomainError.toFieldErrors(): SignUpFieldErrors? {
        val code = when (this) {
            is DomainError.Validation -> code
            is DomainError.Conflict -> code
            else -> null
        }
        return when (code) {
            CODE_INVALID_EMAIL -> SignUpFieldErrors(email = SignUpEmailError.Invalid)
            CODE_EMAIL_TAKEN -> SignUpFieldErrors(email = SignUpEmailError.AlreadyTaken)
            CODE_WEAK_PASSWORD -> SignUpFieldErrors(weakPassword = true)
            CODE_ROLE_NOT_DECLARED, CODE_INVALID_ROLE -> SignUpFieldErrors(roleMissing = true)
            CODE_NAME_REQUIRED -> {
                val state = _state.value
                SignUpFieldErrors(
                    givenNames = PersonName.problemOf(state.givenNames) ?: PersonName.Problem.REQUIRED,
                    familyNames = PersonName.problemOf(state.familyNames),
                )
            }
            else -> null
        }
    }

    private fun Set<SignUpProblem>.toFieldErrors() = SignUpFieldErrors(
        givenNames = filterIsInstance<SignUpProblem.GivenNames>().firstOrNull()?.problem,
        familyNames = filterIsInstance<SignUpProblem.FamilyNames>().firstOrNull()?.problem,
        email = if (SignUpProblem.InvalidEmail in this) SignUpEmailError.Invalid else null,
        weakPassword = SignUpProblem.WeakPassword in this,
        roleMissing = SignUpProblem.RoleNotDeclared in this,
    )

    private companion object {
        // Códigos de `extensions.code` (docs/backend/CODIGOS-DE-ERROR.md, Iam · F1).
        const val CODE_INVALID_EMAIL = "InvalidEmail"
        const val CODE_EMAIL_TAKEN = "EmailAlreadyTaken"
        const val CODE_WEAK_PASSWORD = "WeakPassword"
        const val CODE_ROLE_NOT_DECLARED = "RoleNotDeclared"
        const val CODE_INVALID_ROLE = "InvalidRole"
        const val CODE_NAME_REQUIRED = "NameRequired"
    }
}
