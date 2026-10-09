package pe.edu.upc.healthify.core.network.auth

/**
 * Puerto para canjear el refresh token (`POST /authentication/token-refreshes`, IAM-4). Lo implementa el
 * contexto `iam` en su infrastructure, con un cliente HTTP sin [TokenRefreshAuthenticator] (sin recursión).
 */
fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String): TokenRefreshResult
}

sealed interface TokenRefreshResult {
    /** `200`: el par nuevo. El refresh token rota en cada uso. */
    data class Refreshed(val accessToken: String, val refreshToken: String) : TokenRefreshResult

    /** `401` (`RefreshTokenInvalid`): vencido, revocado por sign-out o reusado. La sesión terminó. */
    data object Rejected : TokenRefreshResult

    /** Sin red, timeout o `5xx`: no se sabe si la sesión sigue viva, así que no se cierra. */
    data object Unavailable : TokenRefreshResult
}
