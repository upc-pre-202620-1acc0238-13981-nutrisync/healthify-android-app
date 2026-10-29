package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import javax.inject.Inject

/** Result of creating the account: creating and then signing in are two calls that fail separately. */
sealed interface RegisterResult {
    /** Account created and signed in: shell loading comes next. */
    data class SignedIn(val user: SessionUser) : RegisterResult

    /** The account exists but the automatic sign-in failed: the person signs in from the sign-in screen. */
    data class CreatedWithoutSession(val error: DomainError) : RegisterResult

    /** The account was not created. */
    data class Failed(val error: DomainError) : RegisterResult
}

/** `POST /authentication/sign-up` and, if it returns `201`, `POST /authentication/sign-in`. */
class RegisterUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(account: NewAccount): RegisterResult {
        val signUp = authenticationRepository.signUp(account)
        signUp.domainErrorOrNull()?.let { return RegisterResult.Failed(it) }

        val credentials = Credentials.parse(account.email.value, account.password.value)
            ?: return RegisterResult.CreatedWithoutSession(DomainError.Unexpected())
        val signIn = authenticationRepository.signIn(credentials)
        return signIn.fold(
            onSuccess = { RegisterResult.SignedIn(it) },
            onFailure = { RegisterResult.CreatedWithoutSession(signIn.domainErrorOrNull() ?: DomainError.Unexpected()) },
        )
    }
}
