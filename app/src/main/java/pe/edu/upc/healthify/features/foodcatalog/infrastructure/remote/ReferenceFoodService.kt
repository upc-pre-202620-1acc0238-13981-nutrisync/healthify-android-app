package pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote

import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.CreateLocalOverrideRequestDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.ReferenceFoodDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** FoodCatalog. Search is anonymous on the backend; the bearer the client adds changes nothing. */
interface ReferenceFoodService {

    /** Local-first on the server; `200` with an empty list if there are no results. */
    @GET("reference-foods")
    suspend fun search(@Query("query") query: String, @Query("max") max: Int): List<ReferenceFoodDto>

    /** *Local Food Catalog* read model (max. 500, overrides first) for offline logging. */
    @GET("patients/{patientId}/local-food-catalog")
    suspend fun getLocalFoodCatalog(@Path("patientId") patientId: Long): List<ReferenceFoodDto>

    /** `201` with the created food (marked as local for good). */
    @POST("reference-foods/local-overrides")
    suspend fun createLocalOverride(@Body body: CreateLocalOverrideRequestDto): ReferenceFoodDto
}
