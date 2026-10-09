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
 * Persistencia de la sesión: tokens cifrados en [SessionTokenStore] y el usuario en Room. Solo lo usa la
 * infrastructure de `iam` (repositorios de sesión y, en S3/S4/S6, el de autenticación al iniciar sesión).
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

    /** Guarda una sesión recién iniciada y pide enviar lo que quedó pendiente de este usuario (PT21.1). */
    suspend fun save(user: SessionUser, accessToken: String, refreshToken: String) {
        tokenStore.save(accessToken, refreshToken)
        // La fecha de creación de la cuenta no cambia: si es el mismo usuario, se conserva la ya leída.
        val createdAt = sessionUserDao.get()?.takeIf { it.userId == user.id.value }?.createdAt
        sessionUserDao.upsert(user.toEntity().copy(createdAt = createdAt))
        syncScheduler.requestSync()
    }

    /** `UserResource.CreatedAt` guardado del usuario [userId], o `null` si aún no se leyó (u otro usuario). */
    suspend fun accountCreatedAt(userId: Long): String? =
        sessionUserDao.get()?.takeIf { it.userId == userId }?.createdAt

    /** Guarda `CreatedAt` si [userId] sigue siendo el usuario con sesión. */
    suspend fun saveAccountCreatedAt(userId: Long, createdAt: String) {
        val row = sessionUserDao.get()?.takeIf { it.userId == userId } ?: return
        sessionUserDao.upsert(row.copy(createdAt = createdAt))
    }

    /** IAM-3: el idioma ya guardado en la cuenta. Sin usuario guardado no hace nada. */
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

    // Una fila que ya no forma un usuario válido (p. ej. de una versión anterior) equivale a no tener sesión.
    private fun SessionUserEntity.toDomainOrNull(): SessionUser? = try {
        toDomain()
    } catch (_: IllegalArgumentException) {
        null
    }
}
