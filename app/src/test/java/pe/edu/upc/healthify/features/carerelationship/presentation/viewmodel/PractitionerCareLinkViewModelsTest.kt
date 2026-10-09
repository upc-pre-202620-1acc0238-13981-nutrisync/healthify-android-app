package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.IssueInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toRoster
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.InvitationDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.PatientRosterItemDto
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InviteError
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePractitionerCareLinkRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.invitation
import pe.edu.upc.healthify.testing.rosterPatient
import pe.edu.upc.healthify.testing.sessionUser
import java.time.Duration

class PractitionerCareLinkViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = FakeSessionRepository(sessionUser(role = UserRole.PRACTITIONER, id = 3))
    private val repository = FakePractitionerCareLinkRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun rosterViewModel() = PatientRosterViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        getPatientRoster = GetPatientRosterUseCase(repository),
        connectivityObserver = connectivity,
    )

    @Test
    fun `PR1-V an empty roster is the first-time state`() {
        repository.rosterResult = Result.success(PatientRoster(emptyList()))

        val state = rosterViewModel().state.value

        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertFalse(state.needsConnection)
    }

    @Test
    fun `PR1 lists the patients and marks the new ones`() {
        repository.rosterResult = Result.success(
            PatientRoster(
                listOf(
                    rosterPatient(1, "Ana Flores"),
                    rosterPatient(2, "Carlos Quispe"),
                    rosterPatient(3, "Luz Ramírez", isNew = true),
                ),
            ),
        )

        val patients = rosterViewModel().state.value.patients!!

        assertEquals(listOf("Ana Flores", "Carlos Quispe", "Luz Ramírez"), patients.map { it.fullName })
        assertEquals(listOf(false, false, true), patients.map { it.isNew })
        assertEquals(103L, patients.last().careLinkId)
    }

    @Test
    fun `PR1-O offline without a previous read asks for a connection and reloads when it comes back`() {
        connectivity.online.value = false
        repository.rosterResult = failureOf(DomainError.Network)
        val viewModel = rosterViewModel()
        assertTrue(viewModel.state.value.needsConnection)

        repository.rosterResult = Result.success(PatientRoster(listOf(rosterPatient(1, "Ana Flores"))))
        connectivity.online.value = true

        assertFalse(viewModel.state.value.needsConnection)
        assertEquals(1, viewModel.state.value.patients?.size)
    }

    @Test
    fun `PR1 keeps the last list when a refresh fails`() {
        repository.rosterResult = Result.success(PatientRoster(listOf(rosterPatient(1, "Ana Flores"))))
        val viewModel = rosterViewModel()

        repository.rosterResult = failureOf(DomainError.Unexpected("HTTP_500"))
        viewModel.onResume()

        assertEquals(1, viewModel.state.value.patients?.size)
        assertFalse(viewModel.state.value.loadFailed)
    }

    @Test
    fun `roster mapper skips unreadable patients and keeps the rest`() {
        val json = Json { ignoreUnknownKeys = true }
        val roster = json.decodeFromString<List<PatientRosterItemDto>>(
            """
            [
              { "patientId": 1, "careLinkId": 11, "fullName": "Ana Flores", "initial": "A",
                "linkedSince": "2026-03-12T10:00:00-05:00", "linkStatus": "Active", "hasBaseline": true,
                "activePlanVersion": 3, "isNew": false, "nextFollowUpAt": null,
                "hasConsultationInProgress": false, "hasOpenReviewItem": false },
              { "patientId": 2, "careLinkId": 12, "fullName": "", "linkedSince": "2026-03-12T10:00:00-05:00" },
              { "patientId": 3, "careLinkId": 13, "fullName": "Luz Ramírez", "linkedSince": "ayer", "isNew": true }
            ]
            """.trimIndent(),
        ).toRoster()

        assertEquals(listOf("Ana Flores"), roster.patients.map { it.fullName })
        assertTrue(roster.patients.single().isLinkActive)
    }

    // ----- PR2 · Generar invitación -----

    private fun inviteViewModel() = InvitePatientViewModel(
        issueInvitation = IssueInvitationUseCase(repository, fixedClock()),
        connectivityObserver = connectivity,
        clock = fixedClock(),
    )

    @Test
    fun `PR2 generates a code valid for 24 hours on entry and counts down`() {
        val viewModel = inviteViewModel()

        val state = viewModel.state.value
        assertFalse(state.isGenerating)
        assertEquals("Q2hhbmdlTWVQbGVhc2VUb2tlbjEyMzQ1Njc4OTA", state.token?.value)
        assertEquals(Duration.ofHours(24), state.remaining)
        assertEquals(listOf(CONSULTATIONS_NOW.plus(Duration.ofHours(24))), repository.issuedExpirations)
    }

    @Test
    fun `PR2 changing the validity issues a new code with it`() {
        val viewModel = inviteViewModel()

        viewModel.onValidityChange(InvitationValidity.ONE_HOUR)

        assertEquals(CONSULTATIONS_NOW.plus(Duration.ofHours(1)), repository.issuedExpirations.last())
        assertEquals(InvitationValidity.ONE_HOUR, viewModel.state.value.validity)
    }

    @Test
    fun `PR2 generate another code issues a second invitation`() {
        val viewModel = inviteViewModel()
        repository.invitationResult = Result.success(invitation(id = 6))

        viewModel.onGenerateAnother()

        assertEquals(2, repository.issuedExpirations.size)
    }

    @Test
    fun `PR2 an expired code is shown as expired`() {
        repository.invitationResult = Result.success(invitation(expiresAt = CONSULTATIONS_NOW))

        val state = inviteViewModel().state.value

        assertTrue(state.isExpired)
        assertEquals(Duration.ZERO, state.remaining)
    }

    @Test
    fun `PR2 offline on entry does not call the backend and shows the notes text`() {
        connectivity.online.value = false

        val state = inviteViewModel().state.value

        assertEquals(InviteError.OFFLINE, state.error)
        assertNull(state.token)
        assertTrue(repository.issuedExpirations.isEmpty())
    }

    @Test
    fun `PR2 errors map to the notes texts`() {
        repository.invitationResult = failureOf(DomainError.Validation("ExpirationDateRequired"))
        assertEquals(InviteError.EXPIRATION_REQUIRED, inviteViewModel().state.value.error)

        repository.invitationResult = failureOf(DomainError.Forbidden("PractitionerOnly"))
        assertEquals(InviteError.GENERIC, inviteViewModel().state.value.error)
    }

    @Test
    fun `invitation without a usable token is unreadable`() {
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            InvitationDto(5, token = null, expiresAt = "2026-10-08T10:00:00-05:00").toDomain()
        }
        val issued = InvitationDto(5, "Q2hhbmdlTWVQbGVhc2VUb2tlbjEyMzQ1Njc4OTA", "2026-10-08T10:00:00-05:00").toDomain()
        assertEquals(5L, issued.invitationId)
    }
}
