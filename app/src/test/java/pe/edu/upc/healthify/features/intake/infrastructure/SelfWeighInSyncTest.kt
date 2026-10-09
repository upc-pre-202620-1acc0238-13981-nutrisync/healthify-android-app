package pe.edu.upc.healthify.features.intake.infrastructure

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.remote.SelfWeighInService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.RecordSelfWeighInRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SelfWeighInDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SelfWeighInSyncOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncSelfWeighInsRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.SyncedSelfWeighInOutcomeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.WeightTrendDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.WeightTrendPointDto
import pe.edu.upc.healthify.features.intake.infrastructure.repository.SelfWeighInRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedSelfWeighInPayload
import pe.edu.upc.healthify.features.intake.infrastructure.sync.SelfWeighInSyncSender
import pe.edu.upc.healthify.testing.fixedClock
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import pe.edu.upc.healthify.core.sync.SyncEvents

/** Autopesajes sin conexión (IN-4): cola, reenvío idempotente y copia offline de la tendencia (IN-5). */
class SelfWeighInSyncTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val dao = FakePendingOperationDao()
    private val syncEvents = SyncEvents()
    private var syncRequests = 0
    private val queue = PendingOperationQueue(dao) { syncRequests++ }
    private val service = FakeSelfWeighInService()
    private val cache = FakeWeightTrendCacheDao()
    private val repository = SelfWeighInRepositoryImpl(service, cache, queue, json, fixedClock, syncEvents)
    private val sender = SelfWeighInSyncSender(service, json)

    private val declared = OffsetDateTime.parse("2026-10-07T07:30:00-05:00")
    private val weighIn = NewSelfWeighIn(
        weight = WeightKg.of(68.4),
        localTimestamp = LocalTimestamp.forNewEntry(declared, Instant.parse("2026-10-07T13:00:00Z")),
        fasted = true,
        clientEntryId = ClientEntryId("2c4e6a8b-1d3f-4b5a-9c7e-0f1a2b3c4d5e"),
    )

    @Test
    fun `online, the weigh-in is recorded with only the fasted question and nothing is queued`() = runTest {
        val outcome = repository.record(PATIENT, weighIn).getOrThrow()

        assertEquals(SelfWeighInOutcome.RECORDED, outcome)
        assertTrue(dao.rows.value.isEmpty())
        val sent = service.records.single()
        assertEquals(68.4, sent.valueKg, 0.0)
        assertTrue(sent.fastedState)
        assertEquals("2026-10-07T07:30:00-05:00", sent.localTimestamp)
        assertEquals(SelfWeighInOutcome.RECORDED, repository.savedNotice.first())
    }

    @Test
    fun `offline, the weigh-in is queued with its client entry id and its exact local time`() = runTest {
        service.failWith = IOException("offline")

        val outcome = repository.record(PATIENT, weighIn).getOrThrow()

        assertEquals(SelfWeighInOutcome.QUEUED, outcome)
        val row = dao.rows.value.single()
        assertEquals(QueuedSelfWeighInPayload.TYPE, row.type)
        assertEquals(weighIn.clientEntryId.value, row.clientEntryId)
        assertEquals("2026-10-07T07:30:00-05:00", row.localTimestamp)
        val payload = json.decodeFromString(QueuedSelfWeighInPayload.serializer(), row.payload)
        assertEquals(68.4, payload.valueKg, 0.0)
        assertTrue(payload.fastedState)
        assertEquals(SelfWeighInOutcome.QUEUED, repository.savedNotice.first())
        assertTrue(syncRequests > 0)
        // PT19 lo muestra con la hora tal como se guardó.
        val pending = repository.observePending(PATIENT).first().single()
        assertEquals(declared, pending.localTimestamp.value)
        assertFalse(pending.isRejected)
    }

    @Test
    fun `saving the same weigh-in again offline never duplicates it in the queue`() = runTest {
        service.failWith = IOException("offline")

        repeat(3) { repository.record(PATIENT, weighIn) }

        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `a server error is reported, not queued`() = runTest {
        service.failWith = HttpException(Response.error<Any>(500, "{}".toResponseBody()))

        val result = repository.record(PATIENT, weighIn)

        assertTrue(result.domainErrorOrNull() is DomainError.Unexpected)
        assertTrue(dao.rows.value.isEmpty())
        assertNull(repository.savedNotice.first())
    }

    @Test
    fun `the engine sends the queue to the synchronization endpoint and empties it idempotently`() = runTest {
        service.failWith = IOException("offline")
        repository.record(PATIENT, weighIn)
        val second = weighIn.copy(clientEntryId = ClientEntryId("7a6b5c4d-3e2f-4a1b-8c9d-0e1f2a3b4c5d"), fasted = false)
        repository.record(PATIENT, second)
        service.failWith = null
        // El primero ya había llegado (reenvío): el backend lo resuelve en silencio como AlreadyPresent.
        service.syncOutcomes = { ids ->
            ids.mapIndexed { i, id -> SyncedSelfWeighInOutcomeDto(id, 100L + i, if (i == 0) "AlreadyPresent" else "Created") }
        }

        val result = PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()

        assertEquals(SyncRunResult.Done, result)
        assertTrue(dao.rows.value.isEmpty())
        val sent = service.syncRequests.single()
        assertEquals(PATIENT.value, sent.patientId)
        assertEquals(listOf(weighIn.clientEntryId.value, second.clientEntryId.value), sent.entries.map { it.clientEntryId })
        assertEquals("2026-10-07T07:30:00-05:00", sent.entries.first().localTimestamp)
        assertEquals(listOf(true, false), sent.entries.map { it.fastedState })
    }

    @Test
    fun `a retried batch resends the same client entry id`() = runTest {
        service.failWith = IOException("offline")
        repository.record(PATIENT, weighIn)

        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()
        service.failWith = null
        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()

        assertEquals(weighIn.clientEntryId.value, service.syncRequests.single().entries.single().clientEntryId)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `a rejected weigh-in stays visible with its reason and is not resent`() = runTest {
        service.failWith = IOException("offline")
        repository.record(PATIENT, weighIn)
        service.failWith = null
        service.syncOutcomes = { ids -> ids.map { SyncedSelfWeighInOutcomeDto(it, null, "Rejected", "ImplausibleWeightValue") } }

        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()
        PendingSyncEngine(dao, setOf(sender), syncEvents) { PATIENT.value }.syncAll()

        val row = dao.rows.value.single()
        assertEquals(PendingOperationEntity.STATUS_REJECTED, row.status)
        assertEquals("ImplausibleWeightValue", row.rejectionCode)
        assertEquals(1, service.syncRequests.size)
        assertEquals("ImplausibleWeightValue", repository.observePending(PATIENT).first().single().rejectionCode)
    }

    @Test
    fun `without network the sender asks to retry later`() = runTest {
        service.failWith = IOException("offline")
        repository.record(PATIENT, weighIn)

        val operations = queue.observe(PATIENT.value, QueuedSelfWeighInPayload.TYPE).first()

        assertEquals(SendBatchResult.RetryLater, sender.send(PATIENT.value, operations))
    }

    @Test
    fun `the trend is saved on the phone and served from there without connection`() = runTest {
        service.trend = trendDto()
        repository.getWeightTrend(PATIENT, 4).getOrThrow()

        service.failWith = IOException("offline")
        val trend = repository.getWeightTrend(PATIENT, 4).getOrThrow()!!

        assertTrue(trend.fromCache)
        assertEquals(Instant.parse("2026-10-07T13:00:00Z"), trend.savedAt)
        assertEquals(3, trend.points.size)
        assertEquals(2, trend.excludedReadingsCount)
    }

    @Test
    fun `no trend yet is not an error and clears the copy`() = runTest {
        service.trend = trendDto()
        repository.getWeightTrend(PATIENT, 4).getOrThrow()
        service.failWith = HttpException(Response.error<Any>(404, "{}".toResponseBody()))

        val result = repository.getWeightTrend(PATIENT, 4)

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertNull(cache.get(PATIENT.value))
    }

    @Test
    fun `offline without a copy the error is returned`() = runTest {
        service.failWith = IOException("offline")

        assertEquals(DomainError.Network, repository.getWeightTrend(PATIENT, 4).domainErrorOrNull())
    }

    @Test
    fun `the trend mapper keeps only the smoothed series and rejects unreadable dates`() {
        val trend = trendDto().toDomainOrNull()!!
        assertEquals(68.1, trend.points.last().smoothedKg, 0.0)
        assertEquals(0.6, trend.changeKgOverRange!!, 0.0)

        assertNull(trendDto().copy(points = listOf(WeightTrendPointDto("ayer", 68.0))).toDomainOrNull())
        assertNull(trendDto().copy(windowSize = 0).toDomainOrNull())
    }

    private fun trendDto() = WeightTrendDto(
        patientId = PATIENT.value,
        windowSize = 7,
        lastRecalculatedAt = "2026-10-07T12:00:00-05:00",
        points = listOf(
            WeightTrendPointDto("2026-09-20", 67.5),
            WeightTrendPointDto("2026-09-28", 67.8),
            WeightTrendPointDto("2026-10-06", 68.1),
        ),
        excludedReadingsCount = 2,
        changeKgOverRange = 0.6,
        slopeKgPerWeek = 0.15,
        rangeFrom = "2026-09-09",
        rangeTo = "2026-10-07",
    )

    private class FakeSelfWeighInService : SelfWeighInService {
        var failWith: Exception? = null
        var trend: WeightTrendDto? = null
        var syncOutcomes: (List<String>) -> List<SyncedSelfWeighInOutcomeDto> =
            { ids -> ids.map { SyncedSelfWeighInOutcomeDto(it, 1, "Created") } }
        val records = mutableListOf<RecordSelfWeighInRequestDto>()
        val syncRequests = mutableListOf<SyncSelfWeighInsRequestDto>()

        private fun check() {
            failWith?.let { throw it }
        }

        override suspend fun record(body: RecordSelfWeighInRequestDto): SelfWeighInDto {
            check()
            records += body
            return SelfWeighInDto(1, body.patientId, body.valueKg, body.localTimestamp, body.fastedState, body.fastedState)
        }

        override suspend fun synchronize(body: SyncSelfWeighInsRequestDto): SelfWeighInSyncOutcomeDto {
            check()
            syncRequests += body
            return SelfWeighInSyncOutcomeDto(patientId = body.patientId, entries = syncOutcomes(body.entries.map { it.clientEntryId }))
        }

        override suspend fun getWeightTrend(patientId: Long, weeks: Int): WeightTrendDto {
            check()
            return trend ?: throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
        }
    }

    private class FakeWeightTrendCacheDao : WeightTrendCacheDao {
        private val rows = mutableMapOf<Long, WeightTrendCacheEntity>()

        override suspend fun get(patientId: Long) = rows[patientId]

        override suspend fun upsert(entity: WeightTrendCacheEntity) {
            rows[entity.patientId] = entity
        }

        override suspend fun delete(patientId: Long) {
            rows.remove(patientId)
        }
    }

    private companion object {
        val PATIENT = PatientId(12)
    }
}
