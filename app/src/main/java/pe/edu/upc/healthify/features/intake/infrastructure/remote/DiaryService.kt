package pe.edu.upc.healthify.features.intake.infrastructure.remote

import okhttp3.MultipartBody
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateAdjustmentRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateConfirmationRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GenerateMealIdeasRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ManualLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeasDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealPhotoAnalysisDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PhotoLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncPendingEntriesRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** IntakeBodyResponse · diario (§5.5). No hay `DELETE` en este contexto. */
interface DiaryService {

    @GET("patients/{patientId}/diary-entries")
    suspend fun getDiaryEntries(@Path("patientId") patientId: Long, @Query("date") date: String): List<DiaryEntryDto>

    /** F17 · `201`. Reenviar el mismo `clientEntryId` devuelve la misma entrada. */
    @POST("diary-entries/manual-logs")
    suspend fun logManualMeal(@Body body: ManualLogRequestDto): DiaryEntryDto

    /** F41 paso 5 · `201`. Reenviar el mismo `clientEntryId` o `analysisId` devuelve la misma entrada. */
    @POST("diary-entries/photo-logs")
    suspend fun logPhotoMeal(@Body body: PhotoLogRequestDto): DiaryEntryDto

    /** IN-6 · `201` con `MealGroupLogResource` (solo se usa el status). */
    @POST("diary-entries/manual-logs/batch")
    suspend fun logMealGroup(@Body body: MealGroupLogRequestDto)

    @POST("diary-entries/{diaryEntryId}/estimate-confirmation")
    suspend fun confirmEstimate(
        @Path("diaryEntryId") diaryEntryId: Long,
        @Body body: EstimateConfirmationRequestDto,
    )

    @POST("diary-entries/{diaryEntryId}/estimate-adjustment")
    suspend fun adjustEstimate(
        @Path("diaryEntryId") diaryEntryId: Long,
        @Body body: EstimateAdjustmentRequestDto,
    )

    /** F20 · idempotente por `clientEntryId`; cada ítem se reporta en `entries`. */
    @POST("diary-entries/synchronization")
    suspend fun synchronize(@Body body: SyncPendingEntriesRequestDto): SyncOutcomeDto
}

/** IN-7 · la foto viaja solo en memoria del servidor y nunca se guarda. */
interface MealPhotoService {

    @Multipart
    @POST("patients/{patientId}/meal-photo-analyses")
    suspend fun analyze(@Path("patientId") patientId: Long, @Part photo: MultipartBody.Part): MealPhotoAnalysisDto
}

/** IA-3 · ideas de comidas. */
interface MealIdeasService {

    @POST("patients/{patientId}/meal-ideas")
    suspend fun generate(@Path("patientId") patientId: Long, @Body body: GenerateMealIdeasRequestDto): MealIdeasDto
}
