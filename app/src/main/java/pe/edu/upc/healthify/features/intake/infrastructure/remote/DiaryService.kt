package pe.edu.upc.healthify.features.intake.infrastructure.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateAdjustmentRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateConfirmationRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GenerateMealIdeasRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ManualLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeasDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealPhotoAnalysisDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PhotoLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PlanFitDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncPendingEntriesRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** IntakeBodyResponse · diary. There is no `DELETE` in this context. */
interface DiaryService {

    @GET("patients/{patientId}/diary-entries")
    suspend fun getDiaryEntries(@Path("patientId") patientId: Long, @Query("date") date: String): List<DiaryEntryDto>

    /** `201`. Resending the same `clientEntryId` returns the same entry. */
    @POST("diary-entries/manual-logs")
    suspend fun logManualMeal(@Body body: ManualLogRequestDto): DiaryEntryDto

    /** `201`. Resending the same `clientEntryId` or `analysisId` returns the same entry. */
    @POST("diary-entries/photo-logs")
    suspend fun logPhotoMeal(@Body body: PhotoLogRequestDto): DiaryEntryDto

    /** `201` with `MealGroupLogResource` (only the status is used). */
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

    /** Idempotent by `clientEntryId`; each item is reported in `entries`. */
    @POST("diary-entries/synchronization")
    suspend fun synchronize(@Body body: SyncPendingEntriesRequestDto): SyncOutcomeDto
}

/** The photo only travels in server memory and is never stored. */
interface MealPhotoService {

    /** [localDate] (`yyyy-MM-dd`) is optional: Retrofit leaves the part out if it is `null`. */
    @Multipart
    @POST("patients/{patientId}/meal-photo-analyses")
    suspend fun analyze(
        @Path("patientId") patientId: Long,
        @Part photo: MultipartBody.Part,
        @Part("localDate") localDate: RequestBody? = null,
    ): MealPhotoAnalysisDto
}

/** Meal ideas. */
interface MealIdeasService {

    @POST("patients/{patientId}/meal-ideas")
    suspend fun generate(@Path("patientId") patientId: Long, @Body body: GenerateMealIdeasRequestDto): MealIdeasDto
}

/** The "Was it in your plan?" suggestion, without AI. */
interface PlanFitService {

    @GET("patients/{patientId}/plan-fit")
    suspend fun check(
        @Path("patientId") patientId: Long,
        @Query("referenceFoodId") referenceFoodId: Long,
        @Query("grams") grams: Double,
        @Query("date") date: String?,
    ): PlanFitDto
}
