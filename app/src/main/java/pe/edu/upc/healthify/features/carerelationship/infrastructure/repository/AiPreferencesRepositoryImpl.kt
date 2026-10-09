package pe.edu.upc.healthify.features.carerelationship.infrastructure.repository

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.AiPreferencesLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toUpdateDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.AiPreferencesService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AiPreferencesDto
import javax.inject.Inject

/**
 * Servidor primero; sin conexión, la última lectura (así la card de ideas y la cámara se deciden igual offline).
 * Un `401`/`403`/`404` es una respuesta del servidor («no hay IA para este paciente»): se guarda como apagada.
 */
class AiPreferencesRepositoryImpl @Inject constructor(
    private val service: AiPreferencesService,
    private val local: AiPreferencesLocalDataSource,
    private val json: Json,
) : AiPreferencesRepository {

    override suspend fun getAiPreferences(patientId: PatientId): AiPreferences {
        val pid = patientId.value
        val remote = apiCall { service.getAiPreferences(pid) }
        remote.getOrNull()?.let { dto ->
            local.save(pid, json.encodeToString(AiPreferencesDto.serializer(), dto))
            return dto.toDomain()
        }
        return when (remote.domainErrorOrNull()) {
            DomainError.Network, is DomainError.Unexpected -> lastKnown(pid) ?: AiPreferences.NoneGranted
            else -> {
                local.save(pid, json.encodeToString(AiPreferencesDto.serializer(), AiPreferencesDto()))
                AiPreferences.NoneGranted
            }
        }
    }

    override suspend fun updateAiPreferences(
        patientId: PatientId,
        preferences: AiPreferences,
    ): Result<AiPreferences> {
        val pid = patientId.value
        val result = apiCall { service.updateAiPreferences(pid, preferences.toUpdateDto()) }
        val dto = result.getOrNull() ?: return domainFailure(result.domainErrorOrNull() ?: DomainError.Unexpected())
        local.save(pid, json.encodeToString(AiPreferencesDto.serializer(), dto))
        return Result.success(dto.toDomain())
    }

    private suspend fun lastKnown(patientId: Long): AiPreferences? {
        val stored = local.lastKnown(patientId) ?: return null
        return try {
            json.decodeFromString(AiPreferencesDto.serializer(), stored).toDomain()
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
