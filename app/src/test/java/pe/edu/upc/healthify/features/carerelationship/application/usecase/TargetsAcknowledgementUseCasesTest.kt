package pe.edu.upc.healthify.features.carerelationship.application.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.TargetsReadStatusDto
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.careLink
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.readStatus

class TargetsAcknowledgementUseCasesTest {

    private val careLinks = FakeCareLinkRepository().apply {
        activeResult = Result.success(careLink(id = 8, isActive = true))
    }

    @Test
    fun `read status comes from the active care link`() = runTest {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = 4))

        val status = GetTargetsReadStatusUseCase(careLinks)(12).getOrThrow()

        assertEquals(4, status.versionAwaitingReview)
    }

    @Test
    fun `without an active link there is no read status`() = runTest {
        careLinks.activeResult = failureOf(DomainError.NotFound("CareLinkNotFound"))

        assertEquals(DomainError.NotFound("CareLinkNotFound"), GetTargetsReadStatusUseCase(careLinks)(12).domainErrorOrNull())
    }

    @Test
    fun `acknowledging sends the version the patient saw on the active link`() = runTest {
        AcknowledgeActiveTargetsUseCase(careLinks)(12, planVersion = 3).getOrThrow()

        assertEquals(listOf(CareLinkId(8) to 3), careLinks.acknowledgements)
    }

    @Test
    fun `nothing pending to acknowledge counts as done`() = runTest {
        careLinks.acknowledgeResult = failureOf(DomainError.Validation("NoPendingTargetsVersion"))

        assertTrue(AcknowledgeActiveTargetsUseCase(careLinks)(12, planVersion = 3).isSuccess)
    }

    @Test
    fun `other acknowledgement errors are reported`() = runTest {
        careLinks.acknowledgeResult = failureOf(DomainError.Forbidden("CareLinkNotActive"))

        assertEquals(
            DomainError.Forbidden("CareLinkNotActive"),
            AcknowledgeActiveTargetsUseCase(careLinks)(12, planVersion = 3).domainErrorOrNull(),
        )
    }

    @Test
    fun `read status mapper only offers a version when the acknowledgement is pending`() {
        val pending = TargetsReadStatusDto(8, 12, pendingTargetsVersion = 4, lastAcknowledgedVersion = 3, hasPendingAcknowledgement = true)
        assertEquals(4, pending.toDomain().versionAwaitingReview)

        val done = pending.copy(hasPendingAcknowledgement = false, lastAcknowledgedAt = "2026-09-04T18:00:00Z")
        assertNull(done.toDomain().versionAwaitingReview)
        assertEquals(java.time.Instant.parse("2026-09-04T18:00:00Z"), done.toDomain().lastAcknowledgedAt)
    }
}
