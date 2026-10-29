package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.flow.Flow

/**
 * Session tokens (access + refresh). Implemented by [EncryptedSessionTokenStore].
 * The `iam` context uses it from its infrastructure to save/clear the session; [AuthInterceptor] reads it and
 * [TokenRefreshCoordinator] rotates it.
 */
interface SessionTokenStore {
    /** `true` while a refresh token is stored (the session can be used or renewed). */
    val hasSession: Flow<Boolean>

    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun save(accessToken: String, refreshToken: String)
    suspend fun clear()
}
