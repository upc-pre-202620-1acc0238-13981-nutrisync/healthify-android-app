package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pe.edu.upc.healthify.core.network.auth.AuthHeaders.bearerToken
import javax.inject.Inject

/**
 * Ante un `401` de una petición que llevó bearer, renueva el par (IAM-4) con [TokenRefreshCoordinator] y la
 * reintenta **una vez** con el access token nuevo. Si la renovación es rechazada devuelve `null`: la petición
 * termina con su 401 (→ `DomainError.Unauthorized`) y [SessionExpiryNotifier] ya emitió el evento hacia S6.
 */
class TokenRefreshAuthenticator @Inject constructor(
    private val coordinator: TokenRefreshCoordinator,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Sin bearer (endpoints anónimos o sin sesión) un 401 no es de token vencido.
        val failedToken = response.request.bearerToken() ?: return null
        // Ya se reintentó con un token renovado y volvió a fallar: no insistir.
        if (response.priorResponse != null) return null

        // OkHttp llama al Authenticator en sus hilos de red, nunca en el main thread.
        val newToken = runBlocking { coordinator.refreshAfter(failedToken) } ?: return null
        return response.request.newBuilder()
            .header(AuthHeaders.AUTHORIZATION, AuthHeaders.bearer(newToken))
            .build()
    }
}
