package pe.edu.upc.healthify.features.monitoring

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.application.usecase.CloseReferralUseCase
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationReminderScheduler
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PractitionerAgendaService
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.PractitionerAgendaRepositoryImpl
import pe.edu.upc.healthify.testing.FakePractitionerAgendaRepository
import pe.edu.upc.healthify.testing.failureOf
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CloseReferralTest {

    private val agenda = FakePractitionerAgendaRepository()
    private val closeReferral = CloseReferralUseCase(agenda)

    private val service = mockk<PractitionerAgendaService>()
    private val repository = PractitionerAgendaRepositoryImpl(service, mockk<ConsultationReminderScheduler>(relaxed = true))

    // ----- Use case -----

    @Test
    fun `closes the referral it is given`() = runTest {
        assertTrue(closeReferral(12).isSuccess)
        assertEquals(listOf(12L), agenda.closedReferrals)
    }

    @Test
    fun `a referral that was already closed counts as closed`() = runTest {
        agenda.closeReferralResult = failureOf(DomainError.Conflict("ReferralAlreadyClosed"))

        assertTrue(closeReferral(12).isSuccess)
    }

    @Test
    fun `a missing referral and a network error stay failures`() = runTest {
        agenda.closeReferralResult = failureOf(DomainError.NotFound("ReferralNotFound"))
        assertEquals(DomainError.NotFound("ReferralNotFound"), closeReferral(12).domainErrorOrNull())

        agenda.closeReferralResult = failureOf(DomainError.Network)
        assertEquals(DomainError.Network, closeReferral(12).domainErrorOrNull())
    }

    // ----- Repository: HTTP to domain -----

    @Test
    fun `200 calls the closure of that referral`() = runTest {
        coEvery { service.closeReferral(12) } returns "{}".toResponseBody()

        assertTrue(repository.closeReferral(12).isSuccess)
        coVerify(exactly = 1) { service.closeReferral(12) }
    }

    @Test
    fun `404 and 409 map to their domain errors`() = runTest {
        coEvery { service.closeReferral(12) } throws http(404)
        assertTrue(repository.closeReferral(12).domainErrorOrNull() is DomainError.NotFound)

        coEvery { service.closeReferral(12) } throws http(409)
        assertTrue(repository.closeReferral(12).domainErrorOrNull() is DomainError.Conflict)
    }

    @Test
    fun `a connection failure maps to the network error`() = runTest {
        coEvery { service.closeReferral(12) } throws IOException("offline")

        assertEquals(DomainError.Network, repository.closeReferral(12).domainErrorOrNull())
    }

    private fun http(status: Int) = HttpException(Response.error<Any>(status, "".toResponseBody()))
}
