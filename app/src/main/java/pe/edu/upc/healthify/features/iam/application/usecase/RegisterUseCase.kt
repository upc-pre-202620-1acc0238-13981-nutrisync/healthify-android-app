package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import javax.inject.Inject

/** Resultado de crear la cuenta (S3): crear y luego entrar son dos llamadas que fallan por separado. */
sealed interface RegisterResult {
    /** Cuenta creada y sesión iniciada: sigue S5. */
    data class SignedIn(val user: SessionUser) : RegisterResult

    /** La cuenta existe pero el inicio de sesión automático falló: la persona entra desde S4. */
    data class CreatedWithoutSession(val error: DomainError) : RegisterResult

    /** No se creó la cuenta. */
    data class Failed(val error: DomainError) : RegisterResult
}

/** F1 → F2: `POST /authentication/sign-up` y, si responde `201`, `POST /authentication/sign-in`. */
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
