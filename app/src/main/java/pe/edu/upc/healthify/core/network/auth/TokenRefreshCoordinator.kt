package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renueva el par de tokens **una sola vez** aunque varias peticiones reciban 401 a la vez.
 *
 * Todas pasan por un [Mutex]. La primera canjea el refresh token; las demás, al entrar, ven que el access token
 * guardado ya no es el que les falló y reutilizan el nuevo sin otra llamada. (El backend tiene una gracia de 30 s
 * para reintentar el mismo refresh, pero fuera de ella el reúso termina la sesión: no se arriesga.)
 */
@Singleton
class TokenRefreshCoordinator @Inject constructor(
    private val tokenStore: SessionTokenStore,
    private val tokenRefresher: TokenRefresher,
    private val sessionExpiryNotifier: SessionExpiryNotifier,
) {

    private val mutex = Mutex()

    /**
     * Devuelve el access token con el que reintentar la petición que falló con [failedAccessToken], o `null` si
     * no hay sesión que renovar (cerrada o expirada).
     *
     * @throws TokenRefreshUnavailableException si el backend no se pudo alcanzar: la petición original falla como
     *   error de red y la sesión se conserva (sin conexión no se pierde la sesión ni la cola de pendientes).
     */
    suspend fun refreshAfter(failedAccessToken: String): String? = mutex.withLock {
        val current = tokenStore.accessToken()
        if (current != null && current != failedAccessToken) return current

        val refreshToken = tokenStore.refreshToken() ?: return null
        when (val result = tokenRefresher.refresh(refreshToken)) {
            is TokenRefreshResult.Refreshed -> {
                tokenStore.save(result.accessToken, result.refreshToken)
                result.accessToken
            }
            TokenRefreshResult.Rejected -> {
                tokenStore.clear()
                sessionExpiryNotifier.notifyExpired()
                null
            }
            TokenRefreshResult.Unavailable -> throw TokenRefreshUnavailableException()
        }
    }
}

/** Es una [IOException] para que OkHttp la propague y `apiCall` la lea como `DomainError.Network`. */
class TokenRefreshUnavailableException : IOException("Token refresh unavailable")
