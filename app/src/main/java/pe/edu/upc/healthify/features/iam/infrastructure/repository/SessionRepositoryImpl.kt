package pe.edu.upc.healthify.features.iam.infrastructure.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.core.network.auth.SessionExpiryNotifier
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val localDataSource: SessionLocalDataSource,
    private val sessionExpiryNotifier: SessionExpiryNotifier,
) : SessionRepository {

    override val currentUser: Flow<SessionUser?> = localDataSource.user

    override val isLoggedIn: Flow<Boolean> = localDataSource.isLoggedIn

    override val sessionExpired: Flow<Unit> = sessionExpiryNotifier.events

    override suspend fun logoutLocally() = localDataSource.clear()
}
