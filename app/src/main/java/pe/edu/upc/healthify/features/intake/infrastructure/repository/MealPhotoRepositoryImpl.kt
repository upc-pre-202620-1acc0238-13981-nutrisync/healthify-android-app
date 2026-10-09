package pe.edu.upc.healthify.features.intake.infrastructure.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.di.IoDispatcher
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.core.network.toOffsetDateTimeOrNull
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.repository.MealPhotoRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.infrastructure.local.PendingMealPhotoDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.PendingMealPhotoEntity
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toIsoString
import pe.edu.upc.healthify.features.intake.infrastructure.photo.MealPhotoEncoder
import pe.edu.upc.healthify.features.intake.infrastructure.remote.MealPhotoService
import java.io.File
import java.io.IOException
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * F41 · la foto se prepara en el teléfono (≤ 1024 px, JPEG, **sin EXIF**) y se sube en memoria; nunca se guarda en el
 * servidor. En el teléfono solo vive en `cacheDir/meal_photos` y se borra al terminar el flujo.
 */
class MealPhotoRepositoryImpl @Inject constructor(
    private val service: MealPhotoService,
    private val encoder: MealPhotoEncoder,
    private val dao: PendingMealPhotoDao,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MealPhotoRepository {

    override suspend fun analyze(patientId: PatientId, photoPath: String): Result<MealPhotoAnalysis> {
        val jpeg = try {
            encoder.encodeForUpload(photoPath)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            return domainFailure(DomainError.Validation(CODE_UNREADABLE_PHOTO))
        } catch (_: IllegalArgumentException) {
            return domainFailure(DomainError.Validation(CODE_UNREADABLE_PHOTO))
        }
        val part = MultipartBody.Part.createFormData(
            name = PART_NAME,
            filename = UPLOAD_FILE_NAME,
            body = jpeg.toRequestBody(JPEG_MEDIA_TYPE.toMediaType()),
        )
        val result = apiCall { service.analyze(patientId.value, part) }
        val dto = result.getOrElse { return Result.failure(it) }
        return try {
            Result.success(dto.toDomain())
        } catch (_: IllegalArgumentException) {
            domainFailure(DomainError.Unexpected(CODE_INVALID_ANALYSIS))
        }
    }

    override suspend fun savePendingPhoto(
        patientId: PatientId,
        photoPath: String,
        capturedAt: LocalTimestamp,
    ): PendingMealPhoto {
        val id = UUID.randomUUID().toString()
        dao.insert(
            PendingMealPhotoEntity(
                id = id,
                ownerUserId = patientId.value,
                filePath = photoPath,
                capturedAt = capturedAt.toIsoString(),
                createdAtEpochMs = clock.millis(),
            ),
        )
        return PendingMealPhoto(id = id, filePath = photoPath, capturedAt = capturedAt)
    }

    override fun observePendingPhotos(patientId: PatientId): Flow<List<PendingMealPhoto>> =
        dao.observe(patientId.value).map { entities -> entities.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getPendingPhoto(patientId: PatientId, id: String): PendingMealPhoto? =
        dao.get(patientId.value, id)?.toDomainOrNull()

    override suspend fun discardPhoto(photoPath: String) {
        dao.deleteByPath(photoPath)
        // Solo se borran archivos de la carpeta de este flujo: nunca una ruta arbitraria.
        if (encoder.isOwnPhoto(photoPath)) {
            withContext(ioDispatcher) { File(photoPath).delete() }
        }
    }

    private fun PendingMealPhotoEntity.toDomainOrNull(): PendingMealPhoto? {
        val captured = capturedAt.toOffsetDateTimeOrNull() ?: return null
        return PendingMealPhoto(id = id, filePath = filePath, capturedAt = LocalTimestamp.restore(captured))
    }

    private companion object {
        const val PART_NAME = "photo"
        const val UPLOAD_FILE_NAME = "meal.jpg"
        const val JPEG_MEDIA_TYPE = "image/jpeg"

        /** La foto ya no está o no se pudo decodificar: se trata como `UnsupportedPhotoFormat` (tomar otra). */
        const val CODE_UNREADABLE_PHOTO = "UnsupportedPhotoFormat"
        const val CODE_INVALID_ANALYSIS = "INVALID_MEAL_PHOTO_ANALYSIS"
    }
}
