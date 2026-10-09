package pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote

import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.CreateLocalOverrideRequestDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.ReferenceFoodDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** FoodCatalog (§5.4). La búsqueda es anónima en el backend; el bearer que agrega el cliente no cambia nada. */
interface ReferenceFoodService {

    /** F14b · local-first en el servidor; `200` con lista vacía si no hay resultados. */
    @GET("reference-foods")
    suspend fun search(@Query("query") query: String, @Query("max") max: Int): List<ReferenceFoodDto>

    /** Read model *Local Food Catalog* (máx. 500, overrides primero) para registrar sin conexión. */
    @GET("patients/{patientId}/local-food-catalog")
    suspend fun getLocalFoodCatalog(@Path("patientId") patientId: Long): List<ReferenceFoodDto>

    /** F14c · `201` con el alimento creado (marcado como local para siempre). */
    @POST("reference-foods/local-overrides")
    suspend fun createLocalOverride(@Body body: CreateLocalOverrideRequestDto): ReferenceFoodDto
}
