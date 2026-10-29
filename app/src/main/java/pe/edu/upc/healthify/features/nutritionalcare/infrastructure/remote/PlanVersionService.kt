package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanVersionDto
import retrofit2.http.GET
import retrofit2.http.Path

/** The patient side of NutritionalCare. */
interface PlanVersionService {

    /** Owning `Patient` · `200` (empty without a plan) · `403` with no body if it belongs to another patient. */
    @GET("patients/{patientId}/plan-versions")
    suspend fun getPlanVersions(@Path("patientId") patientId: Long): List<PlanVersionDto>
}
