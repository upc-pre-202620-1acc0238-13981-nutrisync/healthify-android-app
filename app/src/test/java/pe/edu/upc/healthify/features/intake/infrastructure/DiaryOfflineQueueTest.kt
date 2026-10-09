package pe.edu.upc.healthify.features.intake.infrastructure

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity
import pe.edu.upc.healthify.core.sync.FakePendingOperationDao
import pe.edu.upc.healthify.core.sync.PendingOperationQueue
import pe.edu.upc.healthify.core.sync.PendingSyncEngine
import pe.edu.upc.healthify.core.sync.SendBatchResult
import pe.edu.upc.healthify.core.sync.SyncRunResult
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PhotoConfirmation
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.remote.DiaryService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateAdjustmentRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.EstimateConfirmationRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ManualLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PhotoLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncPendingEntriesRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncedEntryOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.repository.DiaryRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.sync.DiaryEntrySyncSender
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedDiaryEntryPayload
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.mealPhotoAnalysis
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import pe.edu.upc.healthify.core.sync.SyncEvents

class DiaryOfflineQueueTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val dao = FakePendingOperationDao()
    private val syncEvents = SyncEvents()
    private var syncRequests = 0
    private val queue = PendingOperationQueue(dao) { syncRequests++ }
    private val service = FakeDiaryService()
    private val cache = FakeDiaryDayCacheDao()
    private val repository = DiaryRepositoryImpl(service, cache, queue, { syncRequests++ }, json, fixedClock, syncEvents)
    private val sender = DiaryEntrySyncSender(service, json)

    private val declared = OffsetDateTime.parse("2026-10-07T07:45:00-05:00")
    private val manual = NewManualMeal(
        food = MealFood(7, "Avena con fruta"),
        portion = PortionGrams(250.0),
        localTimestamp = LocalTimestamp.forNewEntry(declared, Instant.parse("2026-10-07T13:00:00Z")),
        planAdherence = PlanAdherence.IN_PLAN,
        clientEntryId = ClientEntryId("3f2a9c1e-5b7d-4e2f-8a1c-9d0e1f2a3b4c"),
    )

    @Test
    fun `online, a manual meal is logged and nothing is queued`() = runTest {
        val outcome = repository.logManualMeal(PATIENT, manual).getOrThrow()

        assertEquals(MealLogOutcome.LOGGED, outcome)
        assertTrue(dao.rows.value.isEmpty())
        assertEquals(manual.clientEntryId.value, service.manualLogs.single().clientEntryId)
        assertEquals("InPlan", service.manualLogs.single().planAdherence)
        assertEquals("Avena con fruta", repository.loggedMealNotice.first()?.description)
    }

    @Test
    fun `offline, the meal is queued with its client entry id and its exact local time`() = runTest {
        service.failWith = IOException("offline")

        val outcome = repository.logManualMeal(PATIENT, manual).getOrThrow()

        assertEquals(MealLogOutcome.QUEUED, outcome)
        val row = dao.rows.value.single()
        assertEquals(manual.clientEntryId.value, row.clientEntryId)
        assertEquals("2026-10-07T07:45:00-05:00", row.localTimestamp)
        assertEquals(QueuedDiaryEntryPayload.TYPE, row.type)
        val payload = json.decodeFromString(QueuedDiaryEntryPayload.serializer(), row.payload)
        assertEquals("2026-10-07T07:45:00-05:00", payload.localTimestamp)
        assertEquals("Manual", payload.provenance)
        assertEquals(MealLogOutcome.QUEUED, repository.loggedMealNotice.first()?.outcome)
        assertTrue(syncRequests > 0)
    }

    @Test
    fun `retrying the same meal offline never duplicates it in the queue`() = runTest {
        service.failWith = IOException("offline")

        repeat(3) { repository.logManualMeal(PATIENT, manual) }

        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `a server error is reported, not queued`() = runTest {
        service.failWith = HttpException(Response.error<Any>(500, "{}".toResponseBody()))

        val result = repository.logManualMeal(PATIENT, manual)

        assertTrue(result.domainErrorOrNull() is DomainError.Unexpected)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `an offline confirmed photo is queued as a confirmed photo with what the patient chose`() = runTest {
        service.failWith = IOException("offline")
        val meal = NewPhotoMeal(
            analysis = mealPhotoAnalysis(),
            confirmation = PhotoConfirmation.Adjusted(MealFood(31, "Tallarín saltado"), PortionGrams(300.0)),
            localTimestamp = manual.localTimestamp,
            planAdherence = PlanAdherence.OFF_PLAN,
            clientEntryId = ClientEntryId.random(),
        )

        repository.logPhotoMeal(PATIENT, meal)

        val payload = json.decodeFromString(QueuedDiaryEntryPayload.serializer(), dao.rows.value.single().payload)
        assertEquals("Photo", payload.provenance)
        assertEquals(31L, payload.referenceFoodId)
        assertEquals(300.0, payload.portionGrams, 0.0)
        assertEquals(0.82, payload.confidence!!, 0.0)
        assertTrue(payload.confirmed)
        assertEquals("OffPlan", payload.planAdherence)
    }

    @Test
    fun `the sender reconciles each item and the engine empties the queue idempotently`() = runTest {
        service.failWith = IOException("offline")
        repository.logManualMeal(PATIENT, manual)
        val second = manual.copy(clientEntryId = ClientEntryId("9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"))
        repository.logManualMeal(PATIENT, second)
        service.failWith = null
        // El primero ya había llegado (reenvío): el backend lo reconoce y no lo duplica.
        service.syncOutcomes = { ids -> ids.mapIndexed { i, id -> SyncedEntryOutcomeDto(id, 100L + i, if (i == 0) "AlreadyPresent" else "Created") } }

        val result = PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()

        assertEquals(SyncRunResult.Done, result)
        assertTrue(dao.rows.value.isEmpty())
        val sent = service.syncRequests.single()
        assertEquals(PATIENT.value, sent.patientId)
        assertEquals(listOf(manual.clientEntryId.value, second.clientEntryId.value), sent.entries.map { it.clientEntryId })
        assertEquals("2026-10-07T07:45:00-05:00", sent.entries.first().localTimestamp)
    }

    @Test
    fun `a rejected item stays visible with its reason and is not resent`() = runTest {
        service.failWith = IOException("offline")
        repository.logManualMeal(PATIENT, manual)
        service.failWith = null
        service.syncOutcomes = { ids -> ids.map { SyncedEntryOutcomeDto(it, null, "Rejected", "LocalTimestampCannotBeRewritten") } }

        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()
        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()

        val row = dao.rows.value.single()
        assertEquals(PendingOperationEntity.STATUS_REJECTED, row.status)
        assertEquals("LocalTimestampCannotBeRewritten", row.rejectionCode)
        assertEquals(1, service.syncRequests.size)
        val pending = repository.observePendingEntries(PATIENT).first().single()
        assertEquals("LocalTimestampCannotBeRewritten", pending.rejectionCode)
        assertEquals(declared, pending.localTimestamp.value)
    }

    @Test
    fun `without network the sender asks to retry later`() = runTest {
        service.failWith = IOException("offline")
        repository.logManualMeal(PATIENT, manual)

        val operations = queue.observe(PATIENT.value, QueuedDiaryEntryPayload.TYPE).first()
        assertEquals(SendBatchResult.RetryLater, sender.send(PATIENT.value, operations))
    }

    @Test
    fun `offline, the diary day comes from the copy saved on the phone`() = runTest {
        val date = LocalDate.parse("2026-10-07")
        service.diaryEntries = listOf(dto(1, "Ceviche"))
        repository.getDiaryDay(PATIENT, date).getOrThrow()

        service.failWith = IOException("offline")
        val day = repository.getDiaryDay(PATIENT, date).getOrThrow()

        assertTrue(day.fromCache)
        assertEquals("Ceviche", day.entries.single().foodName)
        assertTrue(day.entries.single().isPendingConfirmation)
    }

    @Test
    fun `offline without a copy the error is returned`() = runTest {
        service.failWith = IOException("offline")

        val result = repository.getDiaryDay(PATIENT, LocalDate.parse("2026-10-06"))

        assertEquals(DomainError.Network, result.domainErrorOrNull())
        assertNull(result.getOrNull())
    }

    private fun dto(id: Long, name: String) = DiaryEntryDto(
        diaryEntryId = id,
        patientId = PATIENT.value,
        localTimestamp = "2026-10-07T14:10:00-05:00",
        provenance = "Photo",
        proposedReferenceFoodId = 12,
        proposedPortionGrams = 280.0,
        confidence = 0.64,
        planAdherence = "NotAnswered",
        foodName = name,
    )

    private class FakeDiaryService : DiaryService {
        var failWith: Exception? = null
        var diaryEntries: List<DiaryEntryDto> = emptyList()
        var syncOutcomes: (List<String>) -> List<SyncedEntryOutcomeDto> = { ids -> ids.map { SyncedEntryOutcomeDto(it, 1, "Created") } }
        val manualLogs = mutableListOf<ManualLogRequestDto>()
        val syncRequests = mutableListOf<SyncPendingEntriesRequestDto>()

        private fun check() {
            failWith?.let { throw it }
        }

        override suspend fun getDiaryEntries(patientId: Long, date: String): List<DiaryEntryDto> {
            check()
            return diaryEntries
        }

        override suspend fun logManualMeal(body: ManualLogRequestDto): DiaryEntryDto {
            check()
            manualLogs += body
            return DiaryEntryDto(diaryEntryId = 1, patientId = body.patientId, localTimestamp = body.localTimestamp, provenance = "Manual")
        }

        override suspend fun logPhotoMeal(body: PhotoLogRequestDto): DiaryEntryDto {
            check()
            return DiaryEntryDto(diaryEntryId = 2, patientId = body.patientId, localTimestamp = body.localTimestamp, provenance = "Photo")
        }

        override suspend fun logMealGroup(body: MealGroupLogRequestDto) = check()

        override suspend fun confirmEstimate(diaryEntryId: Long, body: EstimateConfirmationRequestDto) = check()

        override suspend fun adjustEstimate(diaryEntryId: Long, body: EstimateAdjustmentRequestDto) = check()

        override suspend fun synchronize(body: SyncPendingEntriesRequestDto): SyncOutcomeDto {
            check()
            syncRequests += body
            return SyncOutcomeDto(patientId = body.patientId, entries = syncOutcomes(body.entries.map { it.clientEntryId }))
        }
    }

    private class FakeDiaryDayCacheDao : DiaryDayCacheDao {
        private val rows = mutableMapOf<Pair<Long, String>, DiaryDayCacheEntity>()

        override suspend fun get(patientId: Long, date: String) = rows[patientId to date]

        override suspend fun upsert(entity: DiaryDayCacheEntity) {
            rows[entity.patientId to entity.date] = entity
        }

        override suspend fun deleteOlderThan(patientId: Long, oldestDate: String) {
            rows.keys.removeAll { it.first == patientId && it.second < oldestDate }
        }
    }

    private companion object {
        val PATIENT = PatientId(12)
    }
}
