package pe.edu.upc.healthify.core.sync

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity

class PendingSyncEngineTest {

    private val dao = FakePendingOperationDao()
    private val syncEvents = SyncEvents()
    private var scheduled = 0
    private val queue = PendingOperationQueue(dao) { scheduled++ }

    private class RecordingSender(
        override val type: String,
        override val maxBatchSize: Int = 50,
        private val respond: (List<PendingOperation>) -> SendBatchResult = { ops ->
            SendBatchResult.Delivered(accepted = ops.map { it.clientEntryId }.toSet())
        },
    ) : PendingOperationSender {
        val batches = mutableListOf<List<PendingOperation>>()

        override suspend fun send(ownerUserId: Long, operations: List<PendingOperation>): SendBatchResult {
            batches += operations
            return respond(operations)
        }
    }

    private fun engine(vararg senders: PendingOperationSender, activeUserId: Long? = USER) =
        PendingSyncEngine(dao, senders.toSet(), syncEvents) { activeUserId }

    @Test
    fun `enqueue keeps the client entry id, is idempotent and schedules a sync`() = runTest {
        val id = queue.enqueue(DIARY, USER, payload = "{}", localTimestamp = "2026-10-07T08:00:00-05:00")
        queue.enqueue(DIARY, USER, payload = "{}", localTimestamp = "2026-10-07T08:00:00-05:00", clientEntryId = id)

        assertEquals(1, dao.rows.value.size)
        assertEquals(id, dao.rows.value.single().clientEntryId)
        assertEquals(id, java.util.UUID.fromString(id).toString())
        assertEquals(2, scheduled)
    }

    @Test
    fun `accepted operations leave the queue in batches`() = runTest {
        repeat(5) { queue.enqueue(DIARY, USER, "{}", "t$it", clientEntryId = "id-$it") }
        val sender = RecordingSender(DIARY, maxBatchSize = 2)

        val result = engine(sender).syncAll()

        assertEquals(SyncRunResult.Done, result)
        assertEquals(listOf(2, 2, 1), sender.batches.map { it.size })
        assertEquals(listOf("id-0", "id-1"), sender.batches.first().map { it.clientEntryId })
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `resends always use the same client entry id`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "same")
        val sender = RecordingSender(DIARY) { SendBatchResult.RetryLater }

        engine(sender).syncAll()
        engine(sender).syncAll()

        assertEquals(listOf("same", "same"), sender.batches.flatten().map { it.clientEntryId })
        assertEquals(2, dao.row("same")?.attempts)
    }

    @Test
    fun `transient failure keeps everything and asks for a retry`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "a")

        val result = engine(RecordingSender(DIARY) { SendBatchResult.RetryLater }).syncAll()

        assertEquals(SyncRunResult.Retry, result)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.row("a")?.status)
    }

    @Test
    fun `rejected items stay visible but are not retried, unreported ones are retried`() = runTest {
        listOf("ok", "bad", "lost").forEach { queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = it) }
        val sender = RecordingSender(DIARY) {
            SendBatchResult.Delivered(accepted = setOf("ok"), rejected = mapOf("bad" to "LocalTimestampCannotBeRewritten"))
        }

        val result = engine(sender).syncAll()

        assertEquals(SyncRunResult.Retry, result)
        assertNull(dao.row("ok"))
        assertEquals(PendingOperationEntity.STATUS_REJECTED, dao.row("bad")?.status)
        assertEquals("LocalTimestampCannotBeRewritten", dao.row("bad")?.rejectionCode)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.row("lost")?.status)
        assertEquals(1, sender.batches.size)
    }

    @Test
    fun `accepted operations announce their type so the screens read again`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "d")
        queue.enqueue(WEIGH_IN, USER, "{}", "t", clientEntryId = "w")
        val weighIns = RecordingSender(WEIGH_IN) { SendBatchResult.Delivered(accepted = emptySet(), rejected = mapOf("w" to "X")) }

        syncEvents.deliveredOf(DIARY).test {
            engine(RecordingSender(DIARY), weighIns).syncAll()
            awaitItem()
            expectNoEvents()
        }
    }

    @Test
    fun `nothing accepted announces nothing`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "a")
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "b")

        syncEvents.deliveredOf(DIARY).test {
            engine(RecordingSender(DIARY) { SendBatchResult.RetryLater }).syncAll()
            engine(RecordingSender(DIARY) { SendBatchResult.Delivered(accepted = emptySet(), rejected = mapOf("a" to "X", "b" to "X")) }).syncAll()
            expectNoEvents()
        }
    }

    @Test
    fun `only the active user's operations are sent`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "mine")
        queue.enqueue(DIARY, OTHER_USER, "{}", "t", clientEntryId = "theirs")
        val sender = RecordingSender(DIARY)

        engine(sender).syncAll()

        assertEquals(listOf("mine"), sender.batches.flatten().map { it.clientEntryId })
        assertEquals(listOf("theirs"), dao.rows.value.map { it.clientEntryId })
    }

    @Test
    fun `without session nothing is sent`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "a")
        val sender = RecordingSender(DIARY)

        val result = engine(sender, activeUserId = null).syncAll()

        assertEquals(SyncRunResult.Done, result)
        assertTrue(sender.batches.isEmpty())
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `each type goes to its own sender and unknown types wait`() = runTest {
        queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "d")
        queue.enqueue(WEIGH_IN, USER, "{}", "t", clientEntryId = "w")
        queue.enqueue("future.type", USER, "{}", "t", clientEntryId = "f")
        val diary = RecordingSender(DIARY)
        val weighIns = RecordingSender(WEIGH_IN)

        engine(diary, weighIns).syncAll()

        assertEquals(listOf("d"), diary.batches.flatten().map { it.clientEntryId })
        assertEquals(listOf("w"), weighIns.batches.flatten().map { it.clientEntryId })
        assertEquals(listOf("f"), dao.rows.value.map { it.clientEntryId })
    }

    @Test
    fun `pending count and observed list reflect the queue`() = runTest {
        queue.observePendingCount(USER).test {
            assertEquals(0, awaitItem())
            queue.enqueue(DIARY, USER, "{}", "t", clientEntryId = "a")
            assertEquals(1, awaitItem())
            dao.markRejected("a", "ProvenanceRequired")
            assertEquals(0, awaitItem())
        }
        queue.observe(USER, DIARY).test {
            val item = awaitItem().single()
            assertEquals(PendingOperation.Status.REJECTED, item.status)
            assertEquals("ProvenanceRequired", item.rejectionCode)
        }
    }

    private companion object {
        const val USER = 7L
        const val OTHER_USER = 8L
        const val DIARY = "intake.diary-entry"
        const val WEIGH_IN = "intake.self-weigh-in"
    }
}
