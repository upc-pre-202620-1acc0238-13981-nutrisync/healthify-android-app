package pe.edu.upc.healthify.features.iam.infrastructure.remote

import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.ChangePreferredLanguageRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Endpoints autenticados de Iam (§5.1): van por el Retrofit con bearer y renovación del par (IAM-4). */
interface SessionService {

    /** El `sessionId` sale del token, no del cuerpo (F3). `204`. */
    @POST("authentication/sign-out")
    suspend fun signOut()

    @GET("sessions/{sessionId}/navigation-shell")
    suspend fun getNavigationShell(@Path("sessionId") sessionId: Long): NavigationShellDto

    /** La cuenta propia (`403` si no es la del token). */
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): UserDto

    /** `204` (IAM-3). */
    @PUT("users/{userId}/preferred-language")
    suspend fun changePreferredLanguage(
        @Path("userId") userId: Long,
        @Body body: ChangePreferredLanguageRequestDto,
    )
}
