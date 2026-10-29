package pe.edu.upc.healthify.core.network.auth

import okhttp3.Request

/** The `Authorization` header and the anonymous Iam endpoints, which never carry it. */
internal object AuthHeaders {
    const val AUTHORIZATION = "Authorization"
    private const val BEARER_PREFIX = "Bearer "

    // The bearer is never sent to the anonymous endpoints, so a 401 from sign-in
    // (InvalidCredentials/AccountLocked) or from token-refreshes never triggers a renewal.
    private val ANONYMOUS_PATH_SUFFIXES = listOf(
        "authentication/sign-in",
        "authentication/sign-up",
        "authentication/token-refreshes",
    )

    fun bearer(token: String): String = BEARER_PREFIX + token

    /** The access token the request carried, or `null` if it went out without `Authorization: Bearer`. */
    fun Request.bearerToken(): String? =
        header(AUTHORIZATION)?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)

    fun Request.isAnonymousEndpoint(): Boolean {
        val path = url.encodedPath.trimEnd('/')
        return ANONYMOUS_PATH_SUFFIXES.any { path.endsWith(it) }
    }
}
