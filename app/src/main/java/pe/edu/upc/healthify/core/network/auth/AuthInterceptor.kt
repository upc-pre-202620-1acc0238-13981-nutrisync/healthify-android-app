package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import pe.edu.upc.healthify.core.network.auth.AuthHeaders.isAnonymousEndpoint
import javax.inject.Inject

/**
 * Adds `Authorization: Bearer <access token>` when there is a session, except on the anonymous endpoints.
 * When the backend answers 401, [TokenRefreshAuthenticator] renews the token pair and retries.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStore: SessionTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.isAnonymousEndpoint()) return chain.proceed(original)
        // OkHttp runs interceptors on its network threads, never on the main thread.
        val token = runBlocking { tokenStore.accessToken() }
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder().header(AuthHeaders.AUTHORIZATION, AuthHeaders.bearer(token)).build()
        }
        return chain.proceed(request)
    }
}
