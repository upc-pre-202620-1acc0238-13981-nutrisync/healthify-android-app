package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import javax.inject.Inject

/** F2 · login único (S4): `POST /authentication/sign-in`. La sesión queda guardada en el dispositivo. */
class SignInUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(credentials: Credentials): Result<SessionUser> =
        authenticationRepository.signIn(credentials)
}
