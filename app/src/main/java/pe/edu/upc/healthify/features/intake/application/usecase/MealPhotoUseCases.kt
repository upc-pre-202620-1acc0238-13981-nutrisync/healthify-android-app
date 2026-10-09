package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.repository.MealPhotoRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import javax.inject.Inject

/** PT6 «Usar esta foto» → PT6.1 «Viendo tu foto…» (IN-7: la estimación la hace el servidor). */
class AnalyzeMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    suspend operator fun invoke(patientUserId: Long, photoPath: String): Result<MealPhotoAnalysis> =
        repository.analyze(PatientId(patientUserId), photoPath)
}

/** Sin conexión: la foto queda guardada para analizarla al volver la red. */
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

/** Borra la foto del teléfono (al registrar, al descartarla o al elegir registrar a mano). */
class DiscardMealPhotoUseCase @Inject constructor(
    private val repository: MealPhotoRepository,
) {
    suspend operator fun invoke(photoPath: String) = repository.discardPhoto(photoPath)
}
