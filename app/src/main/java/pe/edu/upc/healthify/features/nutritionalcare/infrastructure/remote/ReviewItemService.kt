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

/** The nutritionist's review inbox. */
interface ReviewItemService {

    @GET("review-items")
    suspend fun getItems(@Query("state") state: String): List<ReviewItemDto>

    @POST("review-items/{reviewItemId}/resolution")
    suspend fun resolve(
        @Path("reviewItemId") reviewItemId: Long,
        @Body body: ResolveReviewItemRequestDto,
    ): ReviewItemDto

    /**
     * `200` with the proposal or `202` + `Retry-After` (ProblemDetails body without a code) while generating: both are
     * success for Retrofit, so the raw body is read and the repository decides by status.
     */
    @GET("review-items/{reviewItemId}/plan-proposal")
    suspend fun getPlanProposal(@Path("reviewItemId") reviewItemId: Long): Response<JsonElement>

    @POST("review-items/{reviewItemId}/plan-proposal/acceptance")
    suspend fun acceptPlanProposal(
        @Path("reviewItemId") reviewItemId: Long,
        @Body body: AcceptPlanProposalRequestDto,
    ): PlanProposalAcceptanceDto
}
