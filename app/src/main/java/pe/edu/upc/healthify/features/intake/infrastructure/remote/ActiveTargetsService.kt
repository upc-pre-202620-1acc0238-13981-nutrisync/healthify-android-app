package pe.edu.upc.healthify.features.intake.infrastructure.remote

import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import retrofit2.http.GET
import retrofit2.http.Path

/** IntakeBodyResponse, lectura de metas (§5.5). */
interface ActiveTargetsService {

    /** `200` con las metas vigentes · `404 ActiveTargetsCacheNotFound` si todavía no hay ninguna publicada. */
    @GET("patients/{patientId}/active-targets")
    suspend fun getActiveTargets(@Path("patientId") patientId: Long): ActiveTargetsDto
}
