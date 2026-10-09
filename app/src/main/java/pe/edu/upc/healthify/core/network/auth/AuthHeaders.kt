package pe.edu.upc.healthify.core.network.auth

import okhttp3.Request

/** Cabecera `Authorization` y los endpoints anónimos de Iam (§5.1 del backend), que nunca la llevan. */
internal object AuthHeaders {
    const val AUTHORIZATION = "Authorization"
    private const val BEARER_PREFIX = "Bearer "

    // DECISIÓN IAM-4: no se manda el bearer a los endpoints anónimos. Así un 401 de sign-in
    // (InvalidCredentials/AccountLocked) o de token-refreshes nunca dispara una renovación.
    private val ANONYMOUS_PATH_SUFFIXES = listOf(
        "authentication/sign-in",
        "authentication/sign-up",
        "authentication/token-refreshes",
    )

    fun bearer(token: String): String = BEARER_PREFIX + token

    /** El access token que llevó la request, o `null` si salió sin `Authorization: Bearer`. */
    fun Request.bearerToken(): String? =
        header(AUTHORIZATION)?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)

    fun Request.isAnonymousEndpoint(): Boolean {
        val path = url.encodedPath.trimEnd('/')
        return ANONYMOUS_PATH_SUFFIXES.any { path.endsWith(it) }
    }
}
