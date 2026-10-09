package pe.edu.upc.healthify.features.intake.infrastructure.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.core.sync.PendingOperation
import pe.edu.upc.healthify.core.sync.PendingOperationQueue
import pe.edu.upc.healthify.core.sync.SyncEvents
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.repository.SelfWeighInRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toQueuedPayload
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.SelfWeighInService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.WeightTrendDto
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedSelfWeighInPayload
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend primero. **Solo sin conexión** (`DomainError.Network`) el autopesaje se encola en la cola offline genérica
 * con su `clientEntryId` y su hora local exacta; `PendingSyncWorker` lo envía a `/self-weigh-ins/synchronization`
 * cuando hay red (IN-4). Un 5xx no encola: PT12.2 muestra el error y el paciente reintenta.
 *
 * La tendencia se guarda en Room (`weight_trend_cache`) cada vez que llega; sin conexión o con 5xx se devuelve esa
 * copia. El `404 WeightTrendNotFound` la borra (la tendencia ya no existe en el backend).
 *
 * `@Singleton`: guarda el aviso del último autopesaje hasta que PT13 lo muestra.
 */
@Singleton
class SelfWeighInRepositoryImpl @Inject constructor(
    private val service: SelfWeighInService,
    private val cacheDao: WeightTrendCacheDao,
    private val queue: PendingOperationQueue,
    private val json: Json,
    private val clock: Clock,
    syncEvents: SyncEvents,
) : SelfWeighInRepository {

    override val syncedWeighIns: Flow<Unit> = syncEvents.deliveredOf(QueuedSelfWeighInPayload.TYPE)

    private val notice = MutableStateFlow<SelfWeighInOutcome?>(null)
    override val savedNotice: Flow<SelfWeighInOutcome?> = notice.asStateFlow()

    override fun consumeSavedNotice() {
        notice.value = null
    }

    override suspend fun record(patientId: PatientId, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome> {
        val result = apiCall { service.record(weighIn.toRequestDto(patientId.value)) }
        val outcome = when {
            result.isSuccess -> SelfWeighInOutcome.RECORDED
            result.domainErrorOrNull() == DomainError.Network -> {
                val payload = weighIn.toQueuedPayload()
                // Encolar dos veces el mismo clientEntryId no lo duplica (la cola lo ignora).
                queue.enqueue(
                    type = QueuedSelfWeighInPayload.TYPE,
                    ownerUserId = patientId.value,
                    payload = json.encodeToString(QueuedSelfWeighInPayload.serializer(), payload),
                    localTimestamp = payload.localTimestamp,
                    clientEntryId = payload.clientEntryId,
                )
                SelfWeighInOutcome.QUEUED
            }
            else -> return Result.failure(requireNotNull(result.exceptionOrNull()))
        }
        notice.value = outcome
        return Result.success(outcome)
    }

    override fun observePending(patientId: PatientId): Flow<List<PendingSelfWeighIn>> =
        queue.observe(patientId.value, QueuedSelfWeighInPayload.TYPE).map { operations ->
            operations.mapNotNull { it.toPendingOrNull() }
        }

    override suspend fun getWeightTrend(patientId: PatientId, weeks: Int): Result<WeightTrend?> {
        val pid = patientId.value
        val remote = apiCall { service.getWeightTrend(pid, weeks) }
        val dto = remote.getOrNull()
        if (dto != null) {
            val trend = dto.toDomainOrNull() ?: return domainFailure(DomainError.Unexpected(CODE_INVALID_RESOURCE))
            cacheDao.upsert(
                WeightTrendCacheEntity(
                    patientId = pid,
                    payloadJson = json.encodeToString(WeightTrendDto.serializer(), dto),
                    savedAtEpochMillis = clock.millis(),
                ),
            )
            return Result.success(trend)
        }
        val error = remote.domainErrorOrNull() ?: DomainError.Unexpected()
        return when (error) {
            is DomainError.NotFound -> {
                cacheDao.delete(pid)
                Result.success(null)
            }
            DomainError.Network, is DomainError.Unexpected -> cachedTrend(pid)?.let { Result.success(it) }
                ?: domainFailure(error)
            else -> domainFailure(error)
        }
    }

    private suspend fun cachedTrend(patientId: Long): WeightTrend? {
        val entity = cacheDao.get(patientId) ?: return null
        val dto = try {
            json.decodeFromString(WeightTrendDto.serializer(), entity.payloadJson)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return dto?.toDomainOrNull(fromCache = true, savedAt = Instant.ofEpochMilli(entity.savedAtEpochMillis))
    }

    private fun PendingOperation.toPendingOrNull(): PendingSelfWeighIn? =
        decodePayload(payload)?.toDomainOrNull(
            rejectionCode = if (status == PendingOperation.Status.REJECTED) {
                rejectionCode ?: CODE_UNKNOWN_REJECTION
            } else {
                null
            },
        )

    private fun decodePayload(payload: String): QueuedSelfWeighInPayload? =
        try {
            json.decodeFromString(QueuedSelfWeighInPayload.serializer(), payload)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val CODE_UNKNOWN_REJECTION = "UnexpectedError"
        const val CODE_INVALID_RESOURCE = "INVALID_WEIGHT_TREND"
    }
}
