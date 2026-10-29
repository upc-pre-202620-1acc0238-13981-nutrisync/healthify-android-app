package pe.edu.upc.healthify.core.network.auth

/**
 * Port to redeem the refresh token (`POST /authentication/token-refreshes`). Implemented by the
 * `iam` context in its infrastructure, with an HTTP client without [TokenRefreshAuthenticator] (no recursion).
 */
fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String): TokenRefreshResult
}

sealed interface TokenRefreshResult {
    /** `200`: the new pair. The refresh token rotates on every use. */
    data class Refreshed(val accessToken: String, val refreshToken: String) : TokenRefreshResult

    /** `401` (`RefreshTokenInvalid`): expired, revoked by sign-out or reused. The session is over. */
    data object Rejected : TokenRefreshResult

    /** No network, timeout or `5xx`: whether the session is still alive is unknown, so it is not closed. */
    data object Unavailable : TokenRefreshResult
}
