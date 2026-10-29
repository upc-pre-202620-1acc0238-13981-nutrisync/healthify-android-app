package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pe.edu.upc.healthify.core.network.auth.AuthHeaders.bearerToken
import javax.inject.Inject

/**
 * On a `401` for a request that carried a bearer, renews the token pair with [TokenRefreshCoordinator] and
 * retries it **once** with the new access token. If the renewal is rejected it returns `null`: the request
 * ends with its 401 (→ `DomainError.Unauthorized`) and [SessionExpiryNotifier] has already emitted the event
 * that opens the session expired screen.
 */
class TokenRefreshAuthenticator @Inject constructor(
    private val coordinator: TokenRefreshCoordinator,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Without a bearer (anonymous endpoints or no session) a 401 does not mean an expired token.
        val failedToken = response.request.bearerToken() ?: return null
        // Already retried with a renewed token and it failed again: do not insist.
        if (response.priorResponse != null) return null

        // OkHttp calls the Authenticator on its network threads, never on the main thread.
        val newToken = runBlocking { coordinator.refreshAfter(failedToken) } ?: return null
        return response.request.newBuilder()
            .header(AuthHeaders.AUTHORIZATION, AuthHeaders.bearer(newToken))
            .build()
    }
}
