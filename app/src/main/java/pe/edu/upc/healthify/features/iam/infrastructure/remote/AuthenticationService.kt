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

/** Anonymous Iam endpoints. Built with the `@AnonymousApi` Retrofit: no bearer or authenticator. */
interface AuthenticationService {

    @POST("authentication/sign-up")
    suspend fun signUp(@Body body: SignUpRequestDto): UserDto

    @POST("authentication/sign-in")
    suspend fun signIn(@Body body: SignInRequestDto): SignInResponseDto

    /** `202` with no body, whether or not the account exists. */
    @POST("authentication/password-reset-requests")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequestDto)

    /** `200` with the token; `400` with `PasswordResetCodeInvalid`. */
    @POST("authentication/password-reset-verifications")
    suspend fun verifyPasswordResetCode(
        @Body body: PasswordResetVerificationRequestDto,
    ): PasswordResetVerificationResponseDto

    /**
     * `204` with no body; `400` with `PasswordResetTokenInvalid`, `WeakPassword` or `PasswordReuseNotAllowed`.
     */
    @POST("authentication/password-resets")
    suspend fun resetPassword(@Body body: PasswordResetDto)

    /** `Response` to tell a 401 (session ended) apart from a transient failure. */
    @POST("authentication/token-refreshes")
    suspend fun refreshTokens(@Body body: TokenRefreshRequestDto): Response<TokenRefreshResponseDto>
}
