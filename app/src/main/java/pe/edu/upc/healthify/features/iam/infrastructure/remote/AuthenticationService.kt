package pe.edu.upc.healthify.features.iam.infrastructure.remote

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

    /** `Response` para distinguir el 401 (sesión terminada) de un fallo transitorio. */
    @POST("authentication/token-refreshes")
    suspend fun refreshTokens(@Body body: TokenRefreshRequestDto): Response<TokenRefreshResponseDto>
}
