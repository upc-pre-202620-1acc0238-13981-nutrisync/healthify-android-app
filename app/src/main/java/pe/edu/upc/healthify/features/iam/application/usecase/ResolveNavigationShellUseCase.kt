package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import javax.inject.Inject

/**
 * The shell the app mounts for the session (`GET /sessions/{sessionId}/navigation-shell`).
 *
 * If the session has no shell (the sign-in shell policy failed, "`navigation-shell` would return a null
 * shell") or there is no connection, the shell for the account's role is used (`NavigationShell.ForRole`, the same
 * backend rule). That way a patient who opens the app offline reaches their cached screens.
 */
class ResolveNavigationShellUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    suspend operator fun invoke(user: SessionUser): Result<NavigationShell> {
        val byRole = NavigationShell.forRole(user.role)
        val result = authenticationRepository.getNavigationShell(user.sessionId)
        return when (result.domainErrorOrNull()) {
            DomainError.Network, is DomainError.NotFound -> Result.success(byRole)
            else -> result.map { it ?: byRole }
        }
    }
}
