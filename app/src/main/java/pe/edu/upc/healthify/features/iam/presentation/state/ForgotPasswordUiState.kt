package pe.edu.upc.healthify.features.iam.presentation.state

/** Password recovery steps. */
enum class ForgotPasswordStep {
    /** The account email is typed. */
    Email,

    /** The received code is typed; the backend verifies it before asking for the password. */
    Code,

    /** Code verified: the new password is typed. */
    NewPassword,

    /** Password changed: all that is left is going back to sign-in. */
    Done,
}

/**
 * Password recovery. The code and the password live in memory only; the verification token is not even
 * in the state (the ViewModel keeps it).
 *
 * @param invalidEmail the email is not shaped like an email (checked before calling the backend).
 * @param invalidCode `PasswordResetCodeInvalid`, or a code that is not six digits.
 * @param weakPassword `WeakPassword`, or a password that does not meet the sign-up rules.
 * @param passwordReused `PasswordReuseNotAllowed`: it is the current password.
 * @param tokenExpired `PasswordResetTokenInvalid`: the time to change it ran out; it goes back to the code step.
 * @param resendInSeconds seconds left before another code can be requested; `0` = it can be now. The backend does
 *   not send another email within a minute either.
 * @param codeResent a new code was requested from the [ForgotPasswordStep.Code] step.
 */
data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.Email,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val invalidEmail: Boolean = false,
    val invalidCode: Boolean = false,
    val weakPassword: Boolean = false,
    val passwordReused: Boolean = false,
    val tokenExpired: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val resendInSeconds: Int = 0,
    val codeResent: Boolean = false,
    val showErrorDialog: Boolean = false,
) {
    override fun toString(): String =
        "ForgotPasswordUiState(step=$step, invalidEmail=$invalidEmail, invalidCode=$invalidCode, " +
            "weakPassword=$weakPassword, passwordReused=$passwordReused, tokenExpired=$tokenExpired, " +
            "isSubmitting=$isSubmitting, isOffline=$isOffline)"
}

sealed interface ForgotPasswordEvent {
    /** "Back to sign in", "back" on the first step, or "Sign in" at the end → sign-in. */
    data object NavigateToSignIn : ForgotPasswordEvent
}
