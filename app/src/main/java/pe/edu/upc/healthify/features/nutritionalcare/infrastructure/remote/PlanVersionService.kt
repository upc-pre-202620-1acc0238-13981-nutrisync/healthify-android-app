package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanVersionDto
import retrofit2.http.GET
import retrofit2.http.Path

/** NutritionalCare del lado del paciente (§5.3, NC-8). */
interface PlanVersionService {

    /** `Patient` dueño · `200` (vacía sin plan) · `403` sin cuerpo si es de otro paciente. */
    @GET("patients/{patientId}/plan-versions")
    suspend fun getPlanVersions(@Path("patientId") patientId: Long): List<PlanVersionDto>
}
