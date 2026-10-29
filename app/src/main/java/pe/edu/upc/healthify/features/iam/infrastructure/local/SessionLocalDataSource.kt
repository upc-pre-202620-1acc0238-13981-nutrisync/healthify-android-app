package pe.edu.upc.healthify.features.iam.infrastructure.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pe.edu.upc.healthify.core.network.auth.SessionTokenStore
import pe.edu.upc.healthify.core.sync.ActiveUserIdProvider
import pe.edu.upc.healthify.core.sync.SyncScheduler
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.iam.infrastructure.mapper.toEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session persistence: tokens encrypted in [SessionTokenStore] and the user in Room. Only used by the `iam`
 * infrastructure (the session repositories and, on sign-up, sign-in and session expiry, the authentication one).
 */
@Singleton
class SessionLocalDataSource @Inject constructor(
    private val tokenStore: SessionTokenStore,
    private val sessionUserDao: SessionUserDao,
    private val syncScheduler: SyncScheduler,
) : ActiveUserIdProvider {

    val user: Flow<SessionUser?> = sessionUserDao.observe().map { it?.toDomainOrNull() }

    val isLoggedIn: Flow<Boolean> =
        combine(tokenStore.hasSession, user) { hasTokens, user -> hasTokens && user != null }

    /** Stores a newly started session and asks to send whatever this user left pending. */
    suspend fun save(user: SessionUser, accessToken: String, refreshToken: String) {
        tokenStore.save(accessToken, refreshToken)
        // The account creation date never changes: for the same user, the one already read is kept.
        val createdAt = sessionUserDao.get()?.takeIf { it.userId == user.id.value }?.createdAt
        sessionUserDao.upsert(user.toEntity().copy(createdAt = createdAt))
        syncScheduler.requestSync()
    }

    /** Stored `UserResource.CreatedAt` of user [userId], or `null` if not read yet (or another user). */
    suspend fun accountCreatedAt(userId: Long): String? =
        sessionUserDao.get()?.takeIf { it.userId == userId }?.createdAt

    /** Stores `CreatedAt` if [userId] is still the signed-in user. */
    suspend fun saveAccountCreatedAt(userId: Long, createdAt: String) {
        val row = sessionUserDao.get()?.takeIf { it.userId == userId } ?: return
        sessionUserDao.upsert(row.copy(createdAt = createdAt))
    }

    /** The language already saved on the account. Does nothing without a stored user. */
    suspend fun updatePreferredLanguage(language: PreferredLanguage) {
        val row = sessionUserDao.get() ?: return
        sessionUserDao.upsert(row.copy(preferredLanguage = language.code))
    }

    suspend fun clear() {
        tokenStore.clear()
        sessionUserDao.clear()
    }

    override suspend fun activeUserId(): Long? =
        if (isLoggedIn.first()) sessionUserDao.get()?.userId else null

    // A row that no longer forms a valid user (e.g. from an older version) counts as having no session.
    private fun SessionUserEntity.toDomainOrNull(): SessionUser? = try {
        toDomain()
    } catch (_: IllegalArgumentException) {
        null
    }
}
