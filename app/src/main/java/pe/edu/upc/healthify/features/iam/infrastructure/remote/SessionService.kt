package pe.edu.upc.healthify.features.iam.infrastructure.remote

import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.ChangePreferredLanguageRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Authenticated Iam endpoints: they go through the Retrofit with bearer and token pair refresh. */
interface SessionService {

    /** The `sessionId` comes from the token, not the body. `204`. */
    @POST("authentication/sign-out")
    suspend fun signOut()

    @GET("sessions/{sessionId}/navigation-shell")
    suspend fun getNavigationShell(@Path("sessionId") sessionId: Long): NavigationShellDto

    /** The caller's own account (`403` if it is not the token's). */
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): UserDto

    /** `204`. */
    @PUT("users/{userId}/preferred-language")
    suspend fun changePreferredLanguage(
        @Path("userId") userId: Long,
        @Body body: ChangePreferredLanguageRequestDto,
    )
}
