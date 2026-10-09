package pe.edu.upc.healthify.features.iam.infrastructure.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.network.auth.TokenRefreshResult
import pe.edu.upc.healthify.core.network.auth.TokenRefresher
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.TokenRefreshRequestDto
import java.io.IOException
import javax.inject.Inject

/** Canje del refresh token (IAM-4) para `TokenRefreshCoordinator`. */
class RetrofitTokenRefresher @Inject constructor(
    private val service: AuthenticationService,
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): TokenRefreshResult = try {
        val response = service.refreshTokens(TokenRefreshRequestDto(refreshToken))
        val body = response.body()
        when {
            response.isSuccessful && body != null -> TokenRefreshResult.Refreshed(body.token, body.refreshToken)
            // 401 RefreshTokenInvalid (vencido, revocado o reusado) o 400 (token vacío): no hay sesión que salvar.
            response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_BAD_REQUEST -> TokenRefreshResult.Rejected
            else -> TokenRefreshResult.Unavailable
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        TokenRefreshResult.Unavailable
    } catch (_: SerializationException) {
        TokenRefreshResult.Unavailable
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
    }
}
