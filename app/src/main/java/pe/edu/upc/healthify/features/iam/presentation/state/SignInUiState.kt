package pe.edu.upc.healthify.features.iam.presentation.state

/** Errores de S4 que tienen frame propio. */
enum class SignInError {
    /** S4.1 · `InvalidCredentials` (el mismo exista o no el correo). */
    InvalidCredentials,

    /** S4.2 · `AccountLocked`. */
    AccountLocked,
}

/**
 * S4 · Login único. La contraseña vive solo en memoria.
 *
 * @param isOffline DECISIÓN S4: el Figma no tiene frame sin conexión para S4; se usa el mismo banner
 *   «Necesitas conexión para continuar» de S3 y no se intenta entrar sin red.
 * @param showForgotPasswordDialog DECISIÓN S4/IAM-2: el backend aún no tiene recuperación de contraseña; el enlace
 *   abre un diálogo «Próximamente».
 */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val error: SignInError? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
) {
    override fun toString(): String =
        "SignInUiState(error=$error, isSubmitting=$isSubmitting, isOffline=$isOffline)"
}

sealed interface SignInEvent {
    data object NavigateToShellLoading : SignInEvent
    data object NavigateToSignUp : SignInEvent
}
