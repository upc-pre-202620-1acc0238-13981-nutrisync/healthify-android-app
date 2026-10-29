package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/**
 * Foto del plato (IN-7). La foto **nunca** sale del teléfono con metadatos: antes de subirla se redimensiona a
 * ~1024 px, se comprime a JPEG y se le borra todo el EXIF/XMP/ICC/IPTC. Solo vive en `cacheDir` y se borra al
 * terminar.
 */
interface MealPhotoRepository {

    /**
     * `POST /patients/{pid}/meal-photo-analyses` (multipart `photo` y `localDate`). No borra [photoPath]: la foto sigue
     * disponible para reintentar hasta que la pantalla la [discardPhoto]. Errores: `403 AiConsentRequired`, `413
     * PhotoTooLarge`, `422 PhotoNotRecognized`, `429 AiRateLimited`, `503 AiFeatureDisabled`/`AiProviderUnavailable`.
     *
     * @param localDate el día de la comida: con él la sugerencia de plan (IN-8) pesa lo que queda hoy. Nunca va a la IA.
     */
    suspend fun analyze(patientId: PatientId, photoPath: String, localDate: LocalDate?): Result<MealPhotoAnalysis>

    /** Guarda la foto tomada sin conexión para analizarla al volver la red. */
    suspend fun savePendingPhoto(patientId: PatientId, photoPath: String, capturedAt: LocalTimestamp): PendingMealPhoto

    fun observePendingPhotos(patientId: PatientId): Flow<List<PendingMealPhoto>>

    suspend fun getPendingPhoto(patientId: PatientId, id: String): PendingMealPhoto?

    /** Olvida la foto pendiente (si la hay con ese archivo) y borra el archivo. */
    suspend fun discardPhoto(photoPath: String)
}
