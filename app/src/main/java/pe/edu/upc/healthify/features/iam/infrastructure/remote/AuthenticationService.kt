package pe.edu.upc.healthify.features.iam.infrastructure.remote

import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetVerificationRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetVerificationResponseDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInResponseDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignUpRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.TokenRefreshRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.TokenRefreshResponseDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoints anónimos de Iam (§5.1). Se crea con el Retrofit `@AnonymousApi`: sin bearer ni authenticator. */
interface AuthenticationService {

    @POST("authentication/sign-up")
    suspend fun signUp(@Body body: SignUpRequestDto): UserDto

    @POST("authentication/sign-in")
    suspend fun signIn(@Body body: SignInRequestDto): SignInResponseDto

    /** IAM-2. `202` sin cuerpo, exista o no la cuenta. */
    @POST("authentication/password-reset-requests")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestDto)

    /** IAM-2. `200` con el token; `400` con `PasswordResetCodeInvalid`. */
    @POST("authentication/password-reset-verifications")
    suspend fun verifyPasswordResetCode(
        @Body body: PasswordResetVerificationRequestDto,
    ): PasswordResetVerificationResponseDto

    /**
     * IAM-2. `204` sin cuerpo; `400` con `PasswordResetTokenInvalid`, `WeakPassword` o `PasswordReuseNotAllowed`.
     */
    @POST("authentication/password-resets")
    suspend fun resetPassword(@Body body: PasswordResetDto)

    /** `Response` para distinguir el 401 (sesión terminada) de un fallo transitorio. */
    @POST("authentication/token-refreshes")
    suspend fun refreshTokens(@Body body: TokenRefreshRequestDto): Response<TokenRefreshResponseDto>
}
