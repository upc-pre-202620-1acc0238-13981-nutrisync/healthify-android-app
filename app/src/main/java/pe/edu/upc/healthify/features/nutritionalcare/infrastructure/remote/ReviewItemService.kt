package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import kotlinx.serialization.json.JsonElement
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.AcceptPlanProposalRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanProposalAcceptanceDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ResolveReviewItemRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ReviewItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Bandeja de revisión del nutricionista (F27, F40, NC-10, NC-11). */
interface ReviewItemService {

    @GET("review-items")
    suspend fun getItems(@Query("state") state: String): List<ReviewItemDto>

    @POST("review-items/{reviewItemId}/resolution")
    suspend fun resolve(
        @Path("reviewItemId") reviewItemId: Long,
        @Body body: ResolveReviewItemRequestDto,
    ): ReviewItemDto

    /**
     * `200` con la propuesta o `202` + `Retry-After` (cuerpo ProblemDetails sin código) mientras se genera: los dos son
     * éxito para Retrofit, así que se lee el cuerpo crudo y el repositorio decide por el status.
     */
    @GET("review-items/{reviewItemId}/plan-proposal")
    suspend fun getPlanProposal(@Path("reviewItemId") reviewItemId: Long): Response<JsonElement>

    @POST("review-items/{reviewItemId}/plan-proposal/acceptance")
    suspend fun acceptPlanProposal(
        @Path("reviewItemId") reviewItemId: Long,
        @Body body: AcceptPlanProposalRequestDto,
    ): PlanProposalAcceptanceDto
}
