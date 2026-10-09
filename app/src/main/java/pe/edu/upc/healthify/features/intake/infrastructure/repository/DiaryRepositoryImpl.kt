package pe.edu.upc.healthify.features.intake.infrastructure.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.core.sync.PendingOperation
import pe.edu.upc.healthify.core.sync.PendingOperationQueue
import pe.edu.upc.healthify.core.sync.SyncEvents
import pe.edu.upc.healthify.core.sync.SyncScheduler
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toQueuedPayload
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toUnconfirmedQueuedPayload
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toUnconfirmedRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.DiaryService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateAdjustmentRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateConfirmationRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedDiaryEntryPayload
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend primero. **Solo sin conexión** (`DomainError.Network`) la comida se encola en la cola offline genérica con
 * su `clientEntryId` y su hora local exacta; el `PendingSyncWorker` la envía a `…/synchronization` cuando hay red.
 * Un 5xx no encola: la pantalla muestra «No se pudo registrar» y el paciente reintenta con el **mismo**
 * `clientEntryId` (el backend devuelve la misma entrada si la primera llegó).
 *
 * `@Singleton`: guarda el aviso del último registro hasta que el diario lo muestra.
 */
@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val service: DiaryService,
    private val cacheDao: DiaryDayCacheDao,
    private val queue: PendingOperationQueue,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
    private val clock: Clock,
    syncEvents: SyncEvents,
) : DiaryRepository {

    override val syncedEntries: Flow<Unit> = syncEvents.deliveredOf(QueuedDiaryEntryPayload.TYPE)

    private val notice = MutableStateFlow<LoggedMealNotice?>(null)
    override val loggedMealNotice: Flow<LoggedMealNotice?> = notice.asStateFlow()

    override fun consumeLoggedMealNotice() {
        notice.value = null
    }

    override suspend fun getDiaryDay(patientId: PatientId, date: LocalDate): Result<DiaryDay> {
        val pid = patientId.value
        val remote = apiCall { service.getDiaryEntries(pid, date.toString()) }
        val dtos = remote.getOrNull()
        if (dtos != null) {
            cacheDao.upsert(
                DiaryDayCacheEntity(
                    patientId = pid,
                    date = date.toString(),
                    payloadJson = json.encodeToString(ENTRIES_SERIALIZER, dtos),
                    savedAtEpochMillis = clock.millis(),
                ),
            )
            cacheDao.deleteOlderThan(pid, LocalDate.now(clock).minusDays(CACHED_DAYS).toString())
            return Result.success(DiaryDay(date = date, entries = dtos.mapNotNull { it.toDomainOrNull() }))
        }
        val cached = cacheDao.get(pid, date.toString())?.let { entity ->
            decodeEntries(entity.payloadJson)?.let { entries ->
                DiaryDay(
                    date = date,
                    entries = entries.mapNotNull { it.toDomainOrNull() },
                    fromCache = true,
                    savedAt = Instant.ofEpochMilli(entity.savedAtEpochMillis),
                )
            }
        }
        return if (cached != null) Result.success(cached) else Result.failure(requireNotNull(remote.exceptionOrNull()))
    }

    override fun observePendingEntries(patientId: PatientId): Flow<List<PendingDiaryEntry>> =
        queue.observe(patientId.value, QueuedDiaryEntryPayload.TYPE).map { operations ->
            operations.mapNotNull { it.toPendingEntryOrNull() }
        }

    override suspend fun logManualMeal(patientId: PatientId, meal: NewManualMeal): Result<MealLogOutcome> =
        logOrQueue(
            patientId = patientId,
            send = { service.logManualMeal(meal.toRequestDto(patientId.value)) },
            queued = { listOf(meal.toQueuedPayload()) },
            notice = { outcome -> LoggedMealNotice(meal.food.name, meal.portion.value, outcome) },
        )

    override suspend fun logPhotoMeal(patientId: PatientId, meal: NewPhotoMeal): Result<MealLogOutcome> =
        logOrQueue(
            patientId = patientId,
            send = { service.logPhotoMeal(meal.toRequestDto(patientId.value)) },
            queued = { listOf(meal.toQueuedPayload()) },
            notice = { outcome -> LoggedMealNotice(meal.loggedFood.name, meal.loggedGrams, outcome) },
        )

    override suspend fun keepPhotoMealUnconfirmed(
        patientId: PatientId,
        analysis: MealPhotoAnalysis,
        localTimestamp: LocalTimestamp,
        clientEntryId: ClientEntryId,
    ): Result<MealLogOutcome> =
        logOrQueue(
            patientId = patientId,
            send = { service.logPhotoMeal(analysis.toUnconfirmedRequestDto(patientId.value, localTimestamp, clientEntryId)) },
            queued = { listOf(analysis.toUnconfirmedQueuedPayload(localTimestamp, clientEntryId)) },
            // Sin Snackbar: la comida queda «Por confirmar», no registrada.
            notice = { null },
        )

    override suspend fun logMealGroup(patientId: PatientId, group: NewMealGroup): Result<MealLogOutcome> =
        logOrQueue(
            patientId = patientId,
            send = { service.logMealGroup(group.toRequestDto(patientId.value)) },
            queued = { group.items.map { it.toQueuedPayload(group) } },
            notice = { outcome -> LoggedMealNotice(group.description, null, outcome) },
        )

    override suspend fun confirmEstimate(entryId: DiaryEntryId, planAdherence: PlanAdherence): Result<Unit> =
        apiCall { service.confirmEstimate(entryId.value, EstimateConfirmationRequestDto(planAdherence.code)) }

    override suspend fun adjustEstimate(
        entryId: DiaryEntryId,
        food: MealFood,
        portion: PortionGrams,
        planAdherence: PlanAdherence,
    ): Result<Unit> = apiCall {
        service.adjustEstimate(
            entryId.value,
            EstimateAdjustmentRequestDto(food.referenceFoodId, portion.value, planAdherence.code),
        )
    }

    override fun requestSync() = syncScheduler.requestSync()

    private suspend fun logOrQueue(
        patientId: PatientId,
        send: suspend () -> Unit,
        queued: () -> List<QueuedDiaryEntryPayload>,
        notice: (MealLogOutcome) -> LoggedMealNotice?,
    ): Result<MealLogOutcome> {
        val result = apiCall { send() }
        val outcome = when {
            result.isSuccess -> MealLogOutcome.LOGGED
            result.domainErrorOrNull() == DomainError.Network -> {
                queued().forEach { payload ->
                    // Encolar dos veces el mismo clientEntryId no lo duplica (la cola lo ignora).
                    queue.enqueue(
                        type = QueuedDiaryEntryPayload.TYPE,
                        ownerUserId = patientId.value,
                        payload = json.encodeToString(QueuedDiaryEntryPayload.serializer(), payload),
                        localTimestamp = payload.localTimestamp,
                        clientEntryId = payload.clientEntryId,
                    )
                }
                MealLogOutcome.QUEUED
            }
            else -> return Result.failure(requireNotNull(result.exceptionOrNull()))
        }
        notice(outcome)?.let { this.notice.value = it }
        return Result.success(outcome)
    }

    private fun PendingOperation.toPendingEntryOrNull(): PendingDiaryEntry? =
        decodePayload(payload)?.toDomainOrNull(rejectionCode = if (status == PendingOperation.Status.REJECTED) {
            rejectionCode ?: CODE_UNKNOWN_REJECTION
        } else {
            null
        })

    private fun decodePayload(payload: String): QueuedDiaryEntryPayload? =
        try {
            json.decodeFromString(QueuedDiaryEntryPayload.serializer(), payload)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun decodeEntries(payload: String): List<DiaryEntryDto>? =
        try {
            json.decodeFromString(ENTRIES_SERIALIZER, payload)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        val ENTRIES_SERIALIZER = ListSerializer(DiaryEntryDto.serializer())

        /** Días del diario que se guardan para verlos sin conexión (el selector va hacia atrás). */
        const val CACHED_DAYS = 14L
        const val CODE_UNKNOWN_REJECTION = "UnexpectedError"
    }
}
