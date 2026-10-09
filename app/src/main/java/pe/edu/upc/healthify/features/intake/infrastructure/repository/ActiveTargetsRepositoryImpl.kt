package pe.edu.upc.healthify.features.intake.infrastructure.repository

import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDtoOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toEntity
import pe.edu.upc.healthify.features.intake.infrastructure.remote.ActiveTargetsService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Backend primero; la copia de Room cubre la falta de red y los errores del servidor (*Cache Survives Offline*).
 * Un `404` es la única respuesta que dice «no hay metas»: borra la copia para no mostrar metas que ya no rigen.
 */
class ActiveTargetsRepositoryImpl @Inject constructor(
    private val service: ActiveTargetsService,
    private val dao: ActiveTargetsCacheDao,
    private val json: Json,
    private val clock: Clock,
) : ActiveTargetsRepository {

    override suspend fun getActiveTargets(patientId: PatientId): Result<ActiveTargetsLookup> {
        val pid = patientId.value
        val remote = apiCall { service.getActiveTargets(pid) }
        val dto = remote.getOrNull()
        if (dto != null) {
            val targets = dto.toDomainOrNull()
                ?: return cachedOr(pid, domainFailure(DomainError.Unexpected(CODE_INVALID_RESOURCE)))
            dao.upsert(dto.toEntity(json, Instant.now(clock)))
            return Result.success(ActiveTargetsLookup(targets))
        }
        if (remote.domainErrorOrNull() is DomainError.NotFound) {
            dao.delete(pid)
            return Result.success(ActiveTargetsLookup.None)
        }
        return cachedOr(pid, Result.failure(requireNotNull(remote.exceptionOrNull())))
    }

    private suspend fun cachedOr(patientId: Long, failure: Result<ActiveTargetsLookup>): Result<ActiveTargetsLookup> {
        val entity = dao.get(patientId) ?: return failure
        val cached = entity.toDtoOrNull(json)?.toDomainOrNull() ?: return failure
        return Result.success(
            ActiveTargetsLookup(
                targets = cached,
                fromCache = true,
                savedAt = Instant.ofEpochMilli(entity.savedAtEpochMillis),
            ),
        )
    }

    /** Un recurso que rompe las invariantes del dominio no se muestra ni se guarda. */
    private fun ActiveTargetsDto.toDomainOrNull(): ActiveTargets? =
        try {
            toDomain()
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val CODE_INVALID_RESOURCE = "INVALID_ACTIVE_TARGETS"
    }
}
