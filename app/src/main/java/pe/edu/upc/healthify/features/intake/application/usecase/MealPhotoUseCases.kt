package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.repository.MealPhotoRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate
import javax.inject.Inject

/** "Use this photo" -> "Looking at your photo…" (the server makes the estimate). */
class AnalyzeMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    /** @param mealDate the day of the meal, so the plan suggestion weighs what is left that day. */
    suspend operator fun invoke(patientUserId: Long, photoPath: String, mealDate: LocalDate? = null): Result<MealPhotoAnalysis> =
        repository.analyze(PatientId(patientUserId), photoPath, mealDate)
}

/** Offline: the photo is saved to be analyzed when the network returns. */
class SavePendingMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    suspend operator fun invoke(patientUserId: Long, photoPath: String, capturedAt: LocalTimestamp): PendingMealPhoto =
        repository.savePendingPhoto(PatientId(patientUserId), photoPath, capturedAt)
}

class ObservePendingMealPhotosUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<List<PendingMealPhoto>> =
        repository.observePendingPhotos(PatientId(patientUserId))
}

class GetPendingMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    suspend operator fun invoke(patientUserId: Long, id: String): PendingMealPhoto? =
        repository.getPendingPhoto(PatientId(patientUserId), id)
}

/** Deletes the photo from the phone (on logging, on discarding it or on choosing to log by hand). */
class DiscardMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    suspend operator fun invoke(photoPath: String) = repository.discardPhoto(photoPath)
}
