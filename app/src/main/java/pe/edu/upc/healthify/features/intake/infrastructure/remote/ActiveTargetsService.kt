package pe.edu.upc.healthify.features.intake.infrastructure.remote

import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DailyIntakeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** IntakeBodyResponse, lectura de metas (§5.5). */
interface ActiveTargetsService {

    /** `200` con las metas vigentes · `404 ActiveTargetsCacheNotFound` si todavía no hay ninguna publicada. */
    @GET("patients/{patientId}/active-targets")
    suspend fun getActiveTargets(@Path("patientId") patientId: Long): ActiveTargetsDto

    /**
     * IN-9 · lo registrado en el día del paciente y lo que queda de cada meta. `400 InvalidLocalDate` si no es hoy en
     * el teléfono · `404 ActiveTargetsCacheNotFound` sin metas publicadas.
     */
    @GET("patients/{patientId}/daily-intake")
    suspend fun getDailyIntake(@Path("patientId") patientId: Long, @Query("date") date: String): DailyIntakeDto
}
