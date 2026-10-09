package pe.edu.upc.healthify.features.iam.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.iam.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.AuthenticationService
import pe.edu.upc.healthify.features.iam.infrastructure.remote.SessionService
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val sessionService: SessionService,
    private val localDataSource: SessionLocalDataSource,
) : AuthenticationRepository {

    override suspend fun signUp(account: NewAccount): Result<Unit> =
        apiCall { authenticationService.signUp(account.toDto()) }.map { }

    override suspend fun signIn(credentials: Credentials): Result<SessionUser> {
        val response = apiCall { authenticationService.signIn(credentials.toDto()) }
            .getOrElse { return Result.failure(it) }
        val user = try {
            response.toDomain()
        } catch (_: IllegalArgumentException) {
            // La respuesta no forma un usuario válido (p. ej. un rol desconocido): contrato roto, no se guarda nada.
            return domainFailure(DomainError.Unexpected(MALFORMED_RESPONSE))
        }
        localDataSource.save(user, accessToken = response.token, refreshToken = response.refreshToken)
        return Result.success(user)
    }

    override suspend fun signOutRemotely(): Result<Unit> = apiCall { sessionService.signOut() }

    override suspend fun getNavigationShell(sessionId: SessionId): Result<NavigationShell?> =
        apiCall { sessionService.getNavigationShell(sessionId.value) }.map { it.toDomain() }

    private companion object {
        const val MALFORMED_RESPONSE = "MALFORMED_RESPONSE"
    }
}
