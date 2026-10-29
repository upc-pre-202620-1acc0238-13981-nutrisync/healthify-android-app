package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.entity.PasswordReset
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetCode
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import javax.inject.Inject

/** "Forgot your password?": asks for a code to be sent to the email. */
class RequestPasswordResetUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(email: EmailAddress): Result<Unit> =
        authenticationRepository.requestPasswordReset(email)
}

/** Verifies the received code; if it is correct, returns the token that sets the password. */
class VerifyPasswordResetCodeUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(email: EmailAddress, code: PasswordResetCode): Result<PasswordResetToken> =
        authenticationRepository.verifyPasswordResetCode(email, code)
}

/** Sets the new password with the token. Does not sign in: the person signs in afterwards. */
class ResetPasswordUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(reset: PasswordReset): Result<Unit> = authenticationRepository.resetPassword(reset)
}
