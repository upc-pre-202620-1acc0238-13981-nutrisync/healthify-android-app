package pe.edu.upc.healthify.features.iam.presentation.state

/** Pasos de la recuperación de contraseña (IAM-2). */
enum class ForgotPasswordStep {
    /** Se escribe el correo de la cuenta. */
    Email,

    /** Se escribe el código recibido; el backend lo verifica antes de pedir la contraseña. */
    Code,

    /** Código verificado: se escribe la contraseña nueva. */
    NewPassword,

    /** Contraseña cambiada: queda volver a S4. */
    Done,
}

/**
 * IAM-2 · Recuperar contraseña. El código y la contraseña viven solo en memoria; el token de la verificación ni
 * siquiera está en el estado (lo guarda el ViewModel).
 *
 * @param invalidEmail el correo no tiene forma de correo (se valida antes de llamar al backend).
 * @param invalidCode `PasswordResetCodeInvalid`, o un código que no son seis dígitos.
 * @param weakPassword `WeakPassword`, o una contraseña que no cumple las reglas de registro.
 * @param passwordReused `PasswordReuseNotAllowed`: es la contraseña actual.
 * @param tokenExpired `PasswordResetTokenInvalid`: se acabó el tiempo para cambiarla; se vuelve al paso del código.
 * @param resendInSeconds segundos que faltan para poder pedir otro código; `0` = ya se puede. El backend tampoco
 *   envía otro correo antes de un minuto.
 * @param codeResent se pidió un código nuevo desde el paso [ForgotPasswordStep.Code].
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
    /** «Volver a iniciar sesión», «atrás» en el primer paso, o «Iniciar sesión» al terminar → S4. */
    data object NavigateToSignIn : ForgotPasswordEvent
}
