package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import pe.edu.upc.healthify.core.network.auth.AuthHeaders.isAnonymousEndpoint
import javax.inject.Inject

/**
 * Agrega `Authorization: Bearer <access token>` si hay sesión, salvo en los endpoints anónimos.
 * Cuando el backend responde 401, [TokenRefreshAuthenticator] renueva el par (IAM-4) y reintenta.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStore: SessionTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.isAnonymousEndpoint()) return chain.proceed(original)
        // OkHttp corre los interceptores en sus hilos de red, nunca en el main thread.
        val token = runBlocking { tokenStore.accessToken() }
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder().header(AuthHeaders.AUTHORIZATION, AuthHeaders.bearer(token)).build()
        }
        return chain.proceed(request)
    }
}
