package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.entity.PasswordReset
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetCode
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import javax.inject.Inject

/** IAM-2 · «¿Olvidaste tu contraseña?»: pide que se envíe un código al correo. */
class RequestPasswordResetUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(email: EmailAddress): Result<Unit> =
        authenticationRepository.requestPasswordReset(email)
}

/** IAM-2 · Verifica el código recibido; si es correcto, devuelve el token que fija la contraseña. */
class VerifyPasswordResetCodeUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(email: EmailAddress, code: PasswordResetCode): Result<PasswordResetToken> =
        authenticationRepository.verifyPasswordResetCode(email, code)
}

/** IAM-2 · Fija la contraseña nueva con el token. No inicia sesión: la persona entra después con S4. */
class ResetPasswordUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(reset: PasswordReset): Result<Unit> = authenticationRepository.resetPassword(reset)
}
