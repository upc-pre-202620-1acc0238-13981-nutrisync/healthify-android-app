package pe.edu.upc.healthify.features.iam.presentation.state

/** Sign-in errors that have their own frame. */
enum class SignInError {
    /** `InvalidCredentials` (the same whether or not the email exists). */
    InvalidCredentials,

    /** `AccountLocked`. */
    AccountLocked,
}

/**
 * Single sign-in. The password lives in memory only.
 *
 * @param isOffline the Figma has no offline frame for sign-in; it uses the same "You need a connection to
 *   continue" banner as sign-up and does not try to sign in offline.
 */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val error: SignInError? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val showErrorDialog: Boolean = false,
) {
    override fun toString(): String =
        "SignInUiState(error=$error, isSubmitting=$isSubmitting, isOffline=$isOffline)"
}

sealed interface SignInEvent {
    data object NavigateToShellLoading : SignInEvent
    data object NavigateToSignUp : SignInEvent

    /** "Forgot your password?" → password recovery. */
    data object NavigateToForgotPassword : SignInEvent
}
