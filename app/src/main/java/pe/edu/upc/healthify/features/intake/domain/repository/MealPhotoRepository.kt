package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/**
 * Dish photo. The photo **never** leaves the phone with metadata: before upload it is resized to ~1024 px,
 * compressed to JPEG and stripped of all EXIF/XMP/ICC/IPTC. It only lives in `cacheDir` and is deleted when
 * done.
 */
interface MealPhotoRepository {

    /**
     * `POST /patients/{pid}/meal-photo-analyses` (multipart `photo` and `localDate`). Does not delete [photoPath]: the
     * photo stays available for retries until the screen calls [discardPhoto]. Errors: `403 AiConsentRequired`, `413
     * PhotoTooLarge`, `422 PhotoNotRecognized`, `429 AiRateLimited`, `503 AiFeatureDisabled`/`AiProviderUnavailable`.
     *
     * @param localDate the day of the meal, so the plan suggestion weighs what is left today. Never sent to the AI.
     */
    suspend fun analyze(patientId: PatientId, photoPath: String, localDate: LocalDate?): Result<MealPhotoAnalysis>

    /** Saves the photo taken offline to analyze it when the network returns. */
    suspend fun savePendingPhoto(patientId: PatientId, photoPath: String, capturedAt: LocalTimestamp): PendingMealPhoto

    fun observePendingPhotos(patientId: PatientId): Flow<List<PendingMealPhoto>>

    suspend fun getPendingPhoto(patientId: PatientId, id: String): PendingMealPhoto?

    /** Forgets the pending photo (if there is one with that file) and deletes the file. */
    suspend fun discardPhoto(photoPath: String)
}
