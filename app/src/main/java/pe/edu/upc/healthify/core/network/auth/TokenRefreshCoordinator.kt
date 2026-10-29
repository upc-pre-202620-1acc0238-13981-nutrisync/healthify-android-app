package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renews the token pair **only once** even if several requests get a 401 at the same time.
 *
 * They all go through a [Mutex]. The first one redeems the refresh token; the others, once inside, see that the
 * stored access token is no longer the one that failed for them and reuse the new one without another call. (The
 * backend gives a 30 s grace period to retry the same refresh, but outside it reuse ends the session: no risk taken.)
 */
@Singleton
class TokenRefreshCoordinator @Inject constructor(
    private val tokenStore: SessionTokenStore,
    private val tokenRefresher: TokenRefresher,
    private val sessionExpiryNotifier: SessionExpiryNotifier,
) {

    private val mutex = Mutex()

    /**
     * Returns the access token to retry the request that failed with [failedAccessToken], or `null` if
     * there is no session to renew (signed out or expired).
     *
     * @throws TokenRefreshUnavailableException if the backend could not be reached: the original request fails as a
     *   network error and the session is kept (being offline loses neither the session nor the pending queue).
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

/** An [IOException] so OkHttp propagates it and `apiCall` reads it as `DomainError.Network`. */
class TokenRefreshUnavailableException : IOException("Token refresh unavailable")
