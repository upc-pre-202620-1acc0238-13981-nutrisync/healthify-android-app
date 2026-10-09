package pe.edu.upc.healthify.features.iam.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole

/** Error del campo de correo en S3 (frame «S3 · Validaciones»). */
enum class SignUpEmailError { Invalid, AlreadyTaken }

/** Errores por campo (S3.E). `null` = el campo está bien o aún no se validó. */
data class SignUpFieldErrors(
    val givenNames: PersonName.Problem? = null,
    val familyNames: PersonName.Problem? = null,
    val email: SignUpEmailError? = null,
    val weakPassword: Boolean = false,
    val roleMissing: Boolean = false,
)

/**
 * S3 · Registro. La contraseña vive solo en memoria (nunca en `SavedStateHandle`).
 *
 * @param isOffline sin conexión: banner «Necesitas conexión para continuar» y el texto bajo el botón (S3 Sin
 *   conexión). Crear la cuenta no se intenta sin red.
 * @param showErrorDialog error inesperado del servidor («Reintentar» / «Cerrar»).
 */
data class SignUpUiState(
    val givenNames: String = "",
    val familyNames: String = "",
    val email: String = "",
    val password: String = "",
    val role: UserRole? = null,
    val errors: SignUpFieldErrors = SignUpFieldErrors(),
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val showErrorDialog: Boolean = false,
) {
    override fun toString(): String =
        "SignUpUiState(role=$role, errors=$errors, isSubmitting=$isSubmitting, isOffline=$isOffline)"
}

sealed interface SignUpEvent {
    /** Cuenta creada y sesión iniciada → S5. */
    data object NavigateToShellLoading : SignUpEvent

    /** «Ya tengo cuenta», o la cuenta se creó pero el inicio de sesión automático falló → S4. */
    data object NavigateToSignIn : SignUpEvent
}
