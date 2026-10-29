package pe.edu.upc.healthify.features.iam.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole

/** Email field error on sign-up (the "Validations" frame). */
enum class SignUpEmailError { Invalid, AlreadyTaken }

/** Errors per field. `null` = the field is fine or not validated yet. */
data class SignUpFieldErrors(
    val givenNames: PersonName.Problem? = null,
    val familyNames: PersonName.Problem? = null,
    val email: SignUpEmailError? = null,
    val weakPassword: Boolean = false,
    val roleMissing: Boolean = false,
)

/**
 * Sign-up. The password lives in memory only (never in `SavedStateHandle`).
 *
 * @param isOffline offline: the "You need a connection to continue" banner and the text under the button
 *   (offline frame). Creating the account is not attempted offline.
 * @param showErrorDialog unexpected server error ("Retry" / "Close").
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
    /** Account created and signed in → loading. */
    data object NavigateToShellLoading : SignUpEvent

    /** "I already have an account", or the account was created but the automatic sign-in failed → sign-in. */
    data object NavigateToSignIn : SignUpEvent
}
