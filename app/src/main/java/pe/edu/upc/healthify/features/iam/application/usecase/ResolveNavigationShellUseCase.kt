package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import javax.inject.Inject

/**
 * S5: el shell que la app monta para la sesión (`GET /sessions/{sessionId}/navigation-shell`, F2 paso 10).
 *
 * DECISIÓN S5: si la sesión no tiene shell (la política de F2 falló, «`navigation-shell` devolvería un shell
 * nulo») o no hay conexión, se usa el shell del rol de la cuenta (`NavigationShell.ForRole`, la misma regla del
 * backend). Así un paciente que abre la app sin conexión llega a sus pantallas con caché (PT3.O).
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
