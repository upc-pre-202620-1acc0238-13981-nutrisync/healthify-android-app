package pe.edu.upc.healthify.features.carerelationship.infrastructure.repository

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.CareLinkLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toPatientLinkStatus
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.CareLinkService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AcknowledgeActiveTargetsRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.CareLinkDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.ChangeAiProcessingConsentRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.GrantConsentRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.TargetsReadStatusDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.RedeemInvitationRequestDto
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CareLinkRepositoryImplTest {

    private class FakeLocal : CareLinkLocalDataSource {
        val statuses = mutableMapOf<Long, PatientLinkStatus>()
        val pending = mutableMapOf<Long, Long>()
        override suspend fun lastKnownStatus(patientId: Long) = statuses[patientId]
        override suspend fun saveStatus(patientId: Long, status: PatientLinkStatus) {
            statuses[patientId] = status
        }
        override suspend fun pendingCareLinkId(patientId: Long) = pending[patientId]
        override suspend fun savePendingCareLinkId(patientId: Long, careLinkId: Long) {
            pending[patientId] = careLinkId
        }
        override suspend fun clearPendingCareLinkId(patientId: Long) {
            pending.remove(patientId)
        }
    }

    private class FakeService : CareLinkService {
        var active: () -> CareLinkDto = { throw notFound() }
        var byId: (Long) -> CareLinkDto = { throw notFound() }
        var redeem: (RedeemInvitationRequestDto) -> CareLinkDto = { link(id = 8, isActive = false) }
        var grant: (Long, GrantConsentRequestDto) -> CareLinkDto = { id, _ -> link(id = id, isActive = true) }
        var withdraw: (Long) -> Unit = {}
        val redeemed = mutableListOf<RedeemInvitationRequestDto>()
        val granted = mutableListOf<Pair<Long, GrantConsentRequestDto>>()
        val withdrawn = mutableListOf<Long>()
        override suspend fun getActiveCareLink(patientId: Long) = active()
        override suspend fun getCareLink(careLinkId: Long) = byId(careLinkId)
        override suspend fun redeemInvitation(body: RedeemInvitationRequestDto): CareLinkDto {
            redeemed += body
            return redeem(body)
        }
        override suspend fun grantConsent(careLinkId: Long, body: GrantConsentRequestDto): CareLinkDto {
            granted += careLinkId to body
            return grant(careLinkId, body)
        }
        override suspend fun withdrawConsent(careLinkId: Long) {
            withdrawn += careLinkId
            withdraw(careLinkId)
        }
        override suspend fun changeAiProcessingConsent(careLinkId: Long, body: ChangeAiProcessingConsentRequestDto) = Unit
        var readStatus: (Long) -> TargetsReadStatusDto = { TargetsReadStatusDto(it, 21) }
        val acknowledged = mutableListOf<Pair<Long, AcknowledgeActiveTargetsRequestDto>>()
        override suspend fun getTargetsReadStatus(careLinkId: Long) = readStatus(careLinkId)
        override suspend fun acknowledgeActiveTargets(
            careLinkId: Long,
            body: AcknowledgeActiveTargetsRequestDto,
        ): CareLinkDto {
            acknowledged += careLinkId to body
            return link(id = careLinkId, isActive = true)
        }
    }

    private val local = FakeLocal()
    private val service = FakeService()
    private val repository = CareLinkRepositoryImpl(service, local)
    private val patient = PatientId(21)

    @Test
    fun `active link is ACTIVE and is cached`() = runTest {
        service.active = { link(isActive = true, hasConsent = true) }

        assertEquals(PatientLinkStatus.ACTIVE, repository.getPatientLinkStatus(patient).getOrThrow())
        assertEquals(PatientLinkStatus.ACTIVE, local.statuses[21])
    }

    @Test
    fun `no consented link and nothing pending is NO_LINK`() = runTest {
        assertEquals(PatientLinkStatus.NO_LINK, repository.getPatientLinkStatus(patient).getOrThrow())
    }

    @Test
    fun `remembered link without consent is PENDING_CONSENT`() = runTest {
        repository.rememberPendingCareLink(patient, CareLinkId(5))
        service.byId = { id -> link(id = id, isActive = false, hasConsent = false) }

        assertEquals(PatientLinkStatus.PENDING_CONSENT, repository.getPatientLinkStatus(patient).getOrThrow())
        assertEquals(5L, local.pending[21])
    }

    @Test
    fun `remembered link that was revoked or no longer exists is NO_LINK and is forgotten`() = runTest {
        repository.rememberPendingCareLink(patient, CareLinkId(5))
        service.byId = { link(isActive = false, revokedAt = "2026-10-01T10:00:00Z") }
        assertEquals(PatientLinkStatus.NO_LINK, repository.getPatientLinkStatus(patient).getOrThrow())
        assertNull(local.pending[21])

        repository.rememberPendingCareLink(patient, CareLinkId(6))
        service.byId = { throw notFound() }
        assertEquals(PatientLinkStatus.NO_LINK, repository.getPatientLinkStatus(patient).getOrThrow())
    }

    @Test
    fun `offline uses the last known status`() = runTest {
        local.statuses[21] = PatientLinkStatus.ACTIVE
        service.active = { throw IOException("offline") }

        assertEquals(PatientLinkStatus.ACTIVE, repository.getPatientLinkStatus(patient).getOrThrow())
    }

    @Test
    fun `offline without a known status fails with Network`() = runTest {
        service.active = { throw IOException("offline") }

        assertEquals(DomainError.Network, repository.getPatientLinkStatus(patient).domainErrorOrNull())
    }

    @Test
    fun `server errors are not hidden by the cache`() = runTest {
        local.statuses[21] = PatientLinkStatus.ACTIVE
        service.active = { throw HttpException(Response.error<Any>(500, "".toResponseBody())) }

        assertEquals(DomainError.Unexpected("HTTP_500"), repository.getPatientLinkStatus(patient).domainErrorOrNull())
    }

    @Test
    fun `redemption sends the token and the replace flag and maps the new pending link`() = runTest {
        val token = InvitationToken("A".repeat(43))

        val link = repository.redeemInvitation(token, replaceActiveLink = true).getOrThrow()

        assertEquals(RedeemInvitationRequestDto("A".repeat(43), replaceActiveLink = true), service.redeemed.single())
        assertEquals(CareLinkId(8), link.id)
        assertEquals(PatientLinkStatus.PENDING_CONSENT, link.patientStatus)
    }

    @Test
    fun `granting consent sends the versioned scope and the AI decision and caches ACTIVE`() = runTest {
        repository.rememberPendingCareLink(patient, CareLinkId(5))

        val link = repository.grantConsent(patient, CareLinkId(5), ConsentScope.CURRENT, aiProcessingGranted = true)
            .getOrThrow()

        assertEquals(5L to GrantConsentRequestDto(ConsentScope.CURRENT.value, true), service.granted.single())
        assertTrue(link.isActive)
        assertEquals(PatientLinkStatus.ACTIVE, local.statuses[21])
        assertNull(local.pending[21])
    }

    @Test
    fun `granting consent on a closed link forgets the pending link`() = runTest {
        repository.rememberPendingCareLink(patient, CareLinkId(5))
        service.grant = { _, _ -> throw conflict("CareLinkAlreadyRevoked") }

        val result = repository.grantConsent(patient, CareLinkId(5), ConsentScope.CURRENT, aiProcessingGranted = false)

        assertEquals(DomainError.Conflict("CareLinkAlreadyRevoked"), result.domainErrorOrNull())
        assertEquals(PatientLinkStatus.NO_LINK, local.statuses[21])
        assertNull(local.pending[21])
    }

    @Test
    fun `consent already granted means the link is active`() = runTest {
        repository.rememberPendingCareLink(patient, CareLinkId(5))
        service.grant = { _, _ -> throw conflict("ConsentAlreadyGranted") }

        repository.grantConsent(patient, CareLinkId(5), ConsentScope.CURRENT, aiProcessingGranted = false)

        assertEquals(PatientLinkStatus.ACTIVE, local.statuses[21])
    }

    @Test
    fun `withdrawing consent deletes it and caches NO_LINK`() = runTest {
        local.statuses[21] = PatientLinkStatus.ACTIVE

        repository.withdrawConsent(patient, CareLinkId(5)).getOrThrow()

        assertEquals(listOf(5L), service.withdrawn)
        assertEquals(PatientLinkStatus.NO_LINK, local.statuses[21])
    }

    @Test
    fun `mapper reads active, pending and closed links`() {
        assertEquals(PatientLinkStatus.ACTIVE, link(isActive = true, hasConsent = true).toPatientLinkStatus())
        assertEquals(PatientLinkStatus.PENDING_CONSENT, link(isActive = false).toPatientLinkStatus())
        assertEquals(
            PatientLinkStatus.NO_LINK,
            link(isActive = false, dischargedAt = "2026-10-01T10:00:00Z").toPatientLinkStatus(),
        )
        assertEquals(PatientLinkStatus.NO_LINK, link(isActive = false, hasConsent = true).toPatientLinkStatus())
    }

    private companion object {
        fun notFound() = HttpException(
            Response.error<Any>(404, """{"code":"CareLinkNotFound"}""".toResponseBody()),
        )

        fun conflict(code: String) = HttpException(Response.error<Any>(409, """{"code":"$code"}""".toResponseBody()))

        fun link(
            id: Long = 5,
            isActive: Boolean,
            hasConsent: Boolean = false,
            revokedAt: String? = null,
            dischargedAt: String? = null,
        ) = CareLinkDto(
            careLinkId = id,
            patientId = 21,
            practitionerId = 9,
            isActive = isActive,
            hasConsent = hasConsent,
            revokedAt = revokedAt,
            dischargedAt = dischargedAt,
        )
    }

    @Test
    fun `targets acknowledgement posts the plan version to the care link`() = runTest {
        repository.acknowledgeActiveTargets(CareLinkId(8), planVersion = 4).getOrThrow()

        assertEquals(listOf(8L to AcknowledgeActiveTargetsRequestDto(planVersion = 4)), service.acknowledged)
    }

    @Test
    fun `targets read status is read from the care link`() = runTest {
        service.readStatus = { TargetsReadStatusDto(it, 21, pendingTargetsVersion = 4, hasPendingAcknowledgement = true) }

        assertEquals(4, repository.getTargetsReadStatus(CareLinkId(8)).getOrThrow().versionAwaitingReview)
    }
}
