package pe.edu.upc.healthify.features.carerelationship.application.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.VALID_TOKEN
import pe.edu.upc.healthify.testing.careLink
import pe.edu.upc.healthify.testing.failureOf

class CareRelationshipUseCasesTest {

    private val repository = FakeCareLinkRepository()
    private val patient = PatientId(12)

    @Test
    fun `redeeming remembers the new link as pending consent`() = runTest {
        repository.redeemResult = Result.success(careLink(id = 8))

        val link = RedeemInvitationUseCase(repository)(12, VALID_TOKEN, replaceActiveLink = false).getOrThrow()

        assertEquals(CareLinkId(8), link.id)
        assertEquals(CareLinkId(8), repository.pending[patient])
        assertEquals(false, repository.redemptions.single().replaceActiveLink)
    }

    @Test
    fun `redeeming from PT21_V asks to replace the active link`() = runTest {
        RedeemInvitationUseCase(repository)(12, VALID_TOKEN, replaceActiveLink = true)

        assertEquals(true, repository.redemptions.single().replaceActiveLink)
    }

    @Test
    fun `a QR that is not an invitation is rejected on the phone as InvitationNotValid`() = runTest {
        val result = RedeemInvitationUseCase(repository)(12, "https://example.com", replaceActiveLink = false)

        assertEquals(DomainError.Validation("InvitationNotValid"), result.domainErrorOrNull())
        assertTrue(repository.redemptions.isEmpty())
    }

    @Test
    fun `a rejected redemption remembers nothing`() = runTest {
        repository.redeemResult = failureOf(DomainError.Conflict("InvitationAlreadyRedeemed"))

        RedeemInvitationUseCase(repository)(12, VALID_TOKEN, replaceActiveLink = false)

        assertNull(repository.pending[patient])
    }

    @Test
    fun `consent always sends the current versioned scope`() = runTest {
        GrantConsentUseCase(repository)(12, careLinkId = 8, aiProcessingGranted = false)

        val grant = repository.grants.single()
        assertEquals(CareLinkId(8), grant.careLinkId)
        assertEquals(ConsentScope.CURRENT, grant.scope)
        assertEquals(false, grant.aiProcessingGranted)
    }

    @Test
    fun `withdrawal deletes the consent of the active link`() = runTest {
        repository.activeResult = Result.success(careLink(id = 4, isActive = true))

        assertTrue(WithdrawConsentUseCase(repository)(12).isSuccess)
        assertEquals(listOf(CareLinkId(4)), repository.withdrawals)
    }

    @Test
    fun `withdrawal without an active link is NoActiveConsent and closes the link locally`() = runTest {
        repository.activeResult = failureOf(DomainError.NotFound("CareLinkNotFound"))

        val result = WithdrawConsentUseCase(repository)(12)

        assertEquals(DomainError.Forbidden("NoActiveConsent"), result.domainErrorOrNull())
        assertTrue(repository.withdrawals.isEmpty())
        assertEquals(listOf(patient), repository.closed)
    }

    @Test
    fun `NoActiveConsent from the backend also closes the link locally`() = runTest {
        repository.withdrawResult = failureOf(DomainError.Forbidden("NoActiveConsent"))

        val result = WithdrawConsentUseCase(repository)(12)

        assertEquals(DomainError.Forbidden("NoActiveConsent"), result.domainErrorOrNull())
        assertEquals(listOf(patient), repository.closed)
    }

    @Test
    fun `other withdrawal failures are returned as they are`() = runTest {
        repository.withdrawResult = failureOf(DomainError.Network)

        assertEquals(DomainError.Network, WithdrawConsentUseCase(repository)(12).domainErrorOrNull())
        assertTrue(repository.closed.isEmpty())
    }

    @Test
    fun `pending care link is the one remembered for this patient`() = runTest {
        assertNull(GetPendingCareLinkUseCase(repository)(12))
        repository.pending[patient] = CareLinkId(8)
        assertEquals(CareLinkId(8), GetPendingCareLinkUseCase(repository)(12))
    }
}
