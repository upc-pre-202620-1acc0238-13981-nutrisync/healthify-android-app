package pe.edu.upc.healthify.features.intake.infrastructure.remote

import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.RecordSelfWeighInRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SelfWeighInDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SelfWeighInSyncOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncSelfWeighInsRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.WeightTrendDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Self weigh-ins and trend (IntakeBodyResponse). `Patient` only. */
interface SelfWeighInService {

    /** `201` · `400 ImplausibleWeightValue` outside 20–400 kg. */
    @POST("self-weigh-ins")
    suspend fun record(@Body body: RecordSelfWeighInRequestDto): SelfWeighInDto

    /** Idempotent by `clientEntryId`; each reading is reported in `entries`. */
    @POST("self-weigh-ins/synchronization")
    suspend fun synchronize(@Body body: SyncSelfWeighInsRequestDto): SelfWeighInSyncOutcomeDto

    /** `?weeks=` 1–52 · `404 WeightTrendNotFound` while there is no trend. */
    @GET("patients/{patientId}/weight-trend")
    suspend fun getWeightTrend(
        @Path("patientId") patientId: Long,
        @Query("weeks") weeks: Int,
    ): WeightTrendDto
}
