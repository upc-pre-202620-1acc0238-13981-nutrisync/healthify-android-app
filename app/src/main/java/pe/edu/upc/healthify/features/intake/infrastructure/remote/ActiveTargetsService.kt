package pe.edu.upc.healthify.features.intake.infrastructure.remote

import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DailyIntakeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** IntakeBodyResponse, targets read. */
interface ActiveTargetsService {

    /** `200` with the active targets · `404 ActiveTargetsCacheNotFound` if none has been published yet. */
    @GET("patients/{patientId}/active-targets")
    suspend fun getActiveTargets(@Path("patientId") patientId: Long): ActiveTargetsDto

    /**
     * What was logged on the patient's day and what is left of each target. `400 InvalidLocalDate` if it is not today
     * on the phone · `404 ActiveTargetsCacheNotFound` with no published targets.
     */
    @GET("patients/{patientId}/daily-intake")
    suspend fun getDailyIntake(@Path("patientId") patientId: Long, @Query("date") date: String): DailyIntakeDto
}
