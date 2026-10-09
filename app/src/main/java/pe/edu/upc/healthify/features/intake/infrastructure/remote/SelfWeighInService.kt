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

/** Autopesajes y tendencia (IntakeBodyResponse, §5.5). Solo `Patient`. */
interface SelfWeighInService {

    /** F19 · `201` · `400 ImplausibleWeightValue` fuera de 20–400 kg. */
    @POST("self-weigh-ins")
    suspend fun record(@Body body: RecordSelfWeighInRequestDto): SelfWeighInDto

    /** IN-4 · idempotente por `clientEntryId`; cada lectura se reporta en `entries`. */
    @POST("self-weigh-ins/synchronization")
    suspend fun synchronize(@Body body: SyncSelfWeighInsRequestDto): SelfWeighInSyncOutcomeDto

    /** IN-5 · `?weeks=` 1–52 · `404 WeightTrendNotFound` mientras no hay tendencia. */
    @GET("patients/{patientId}/weight-trend")
    suspend fun getWeightTrend(
        @Path("patientId") patientId: Long,
        @Query("weeks") weeks: Int,
    ): WeightTrendDto
}
