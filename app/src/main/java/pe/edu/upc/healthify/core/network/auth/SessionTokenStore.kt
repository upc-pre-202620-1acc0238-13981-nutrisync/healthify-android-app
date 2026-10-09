package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.flow.Flow

/**
 * Tokens de la sesión (IAM-4: access + refresh). Lo implementa [EncryptedSessionTokenStore].
 * El contexto `iam` lo usa desde su infrastructure para guardar/borrar la sesión; [AuthInterceptor] lo lee y
 * [TokenRefreshCoordinator] lo rota.
 */
interface SessionTokenStore {
    /** `true` mientras haya un refresh token guardado (la sesión se puede usar o renovar). */
    val hasSession: Flow<Boolean>

    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun save(accessToken: String, refreshToken: String)
    suspend fun clear()
}
