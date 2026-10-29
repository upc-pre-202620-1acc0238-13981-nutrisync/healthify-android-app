package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Sign out: notifies the backend (`POST /authentication/sign-out`) and then clears the session from the device.
 *
 * The local session is cleared even if the backend does not answer (offline, 404, 409). Signing out never leaves
 * the person stuck; the forgotten refresh token can no longer be used from this device.
 * The pending queue is kept.
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
