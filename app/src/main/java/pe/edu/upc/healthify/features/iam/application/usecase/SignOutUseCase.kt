package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * F3 · cerrar sesión: avisa al backend (`POST /authentication/sign-out`) y luego borra la sesión del dispositivo.
 *
 * DECISIÓN F3: la sesión local se borra aunque el backend no responda (sin conexión, 404, 409). Cerrar sesión
 * nunca deja a la persona atrapada; el refresh token olvidado ya no se puede usar desde este dispositivo.
 * La cola de pendientes se conserva (PT21.1).
 */
class SignOutUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        authenticationRepository.signOutRemotely()
        sessionRepository.logoutLocally()
    }
}
