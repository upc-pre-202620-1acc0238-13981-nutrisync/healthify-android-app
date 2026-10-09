package pe.edu.upc.healthify.features.monitoring

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.monitoring.application.usecase.CancelFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetUpcomingFollowUpsUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.RecordReferralUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.RescheduleFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.SCHEDULED_FOR_MUST_BE_IN_FUTURE
import pe.edu.upc.healthify.features.monitoring.application.usecase.ScheduleFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpMoment
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralField
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralValidation
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduleFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduledFollowUpDto
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.RecordReferralRoute
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.ScheduleFollowUpRoute
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaDialog
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientOption
import pe.edu.upc.healthify.features.monitoring.presentation.state.ScheduleMomentError
import pe.edu.upc.healthify.features.monitoring.presentation.state.SchedulePatientError
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.AgendaEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.PractitionerAgendaViewModel
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.RecordReferralEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.RecordReferralViewModel
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.ScheduleFollowUpEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.ScheduleFollowUpViewModel
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePractitionerAgendaRepository
import pe.edu.upc.healthify.testing.FakePractitionerCareLinkRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.PATIENT_ID
import pe.edu.upc.healthify.testing.agendaVisit
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.rosterPatient
import pe.edu.upc.healthify.testing.sessionUser
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class PractitionerAgendaTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 2026-09-15 10:00 en Lima (UTC-5). */
    private val clock = fixedClock(CONSULTATIONS_NOW)
    private val zone = ZoneOffset.ofHours(-5)
    private val today = LocalDate.of(2026, 9, 15)

    private val agenda = FakePractitionerAgendaRepository()
    private val careLinks = FakePractitionerCareLinkRepository()
    private val connectivity = FakeConnectivityObserver()
    private val session = FakeSessionRepository(sessionUser(role = UserRole.PRACTITIONER, id = 3))

    // ----- Validación de la fecha (MA-5: debe ser futura) -----

    @Test
    fun `a moment that already passed or is right now is not in the future`() {
        assertEquals(FollowUpMoment.NotInFuture, FollowUpMoment.of(today.minusDays(3), LocalTime.of(10, 0), zone, CONSULTATIONS_NOW))
        assertEquals(FollowUpMoment.NotInFuture, FollowUpMoment.of(today, LocalTime.of(9, 30), zone, CONSULTATIONS_NOW))
        assertEquals(FollowUpMoment.NotInFuture, FollowUpMoment.of(today, LocalTime.of(10, 0), zone, CONSULTATIONS_NOW))
    }

    @Test
    fun `a later time today or a future day is valid, in the phone zone`() {
        val laterToday = FollowUpMoment.of(today, LocalTime.of(10, 1), zone, CONSULTATIONS_NOW) as FollowUpMoment.Valid
        assertEquals(Instant.parse("2026-09-15T15:01:00Z"), laterToday.instant)

        val nextWeek = FollowUpMoment.of(today.plusDays(3), LocalTime.of(10, 0), zone, CONSULTATIONS_NOW) as FollowUpMoment.Valid
        assertEquals(Instant.parse("2026-09-18T15:00:00Z"), nextWeek.instant)
    }

    @Test
    fun `without date or time the moment is incomplete`() {
        assertEquals(FollowUpMoment.Incomplete, FollowUpMoment.of(null, LocalTime.NOON, zone, CONSULTATIONS_NOW))
        assertEquals(FollowUpMoment.Incomplete, FollowUpMoment.of(today, null, zone, CONSULTATIONS_NOW))
    }

    @Test
    fun `the use cases refuse a past moment without calling the backend`() = runTest {
        val past = CONSULTATIONS_NOW.minusSeconds(60)
        val schedule = ScheduleFollowUpUseCase(agenda, clock)(
            NewFollowUp(PatientId(PATIENT_ID), past, FollowUpPreparation(emptySet())),
        )
        val reschedule = RescheduleFollowUpUseCase(agenda, clock)(31, past, FollowUpPreparation(emptySet()))

        assertEquals(DomainError.Validation(SCHEDULED_FOR_MUST_BE_IN_FUTURE), schedule.domainErrorOrNull())
        assertEquals(DomainError.Validation(SCHEDULED_FOR_MUST_BE_IN_FUTURE), reschedule.domainErrorOrNull())
        assertTrue(agenda.scheduled.isEmpty())
        assertTrue(agenda.rescheduled.isEmpty())
    }

    // ----- PR17 -----

    private fun scheduleViewModel(args: Map<String, Any> = mapOf(
        ScheduleFollowUpRoute.ARG_PATIENT_ID to PATIENT_ID,
        ScheduleFollowUpRoute.ARG_PATIENT_NAME to "Ana Flores",
    )) = ScheduleFollowUpViewModel(
        savedStateHandle = SavedStateHandle(args),
        scheduleFollowUp = ScheduleFollowUpUseCase(agenda, clock),
        rescheduleFollowUp = RescheduleFollowUpUseCase(agenda, clock),
        getPatientRoster = GetPatientRosterUseCase(careLinks),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        connectivityObserver = connectivity,
        clock = clock,
    )

    @Test
    fun `PR17 E a date that is not in the future shows the error and sends nothing`() {
        val viewModel = scheduleViewModel()

        viewModel.onDateSelected(LocalDate.of(2026, 9, 12))
        viewModel.onTimeSelected(LocalTime.of(10, 0))
        viewModel.onSubmit()

        assertEquals(ScheduleMomentError.NOT_IN_FUTURE, viewModel.state.value.momentError)
        assertTrue(agenda.scheduled.isEmpty())
    }

    @Test
    fun `PR17 schedules with the preparation chosen and an in-person modality`() = runTest {
        val viewModel = scheduleViewModel()

        viewModel.events.test {
            viewModel.onDateSelected(LocalDate.of(2026, 9, 18))
            viewModel.onTimeSelected(LocalTime.of(10, 0))
            viewModel.onPreparationToggle(PreparationCode.BRING_BLOOD_TESTS, true)
            viewModel.onPreparationToggle(PreparationCode.FASTING, true)
            viewModel.onSubmit()
            assertEquals(ScheduleFollowUpEvent.Scheduled(Instant.parse("2026-09-18T15:00:00Z"), rescheduled = false), awaitItem())
        }
        val sent = agenda.scheduled.single()
        assertEquals(PATIENT_ID, sent.patientId.value)
        assertEquals(listOf(PreparationCode.FASTING, PreparationCode.BRING_BLOOD_TESTS), sent.preparation.orderedCodes)
        assertEquals(ConsultationModality.IN_PERSON, sent.modality)
    }

    @Test
    fun `PR17 E the backend 400 ScheduledForMustBeInFuture is shown on the date field`() {
        agenda.scheduleResult = failureOf(DomainError.Validation(SCHEDULED_FOR_MUST_BE_IN_FUTURE))
        val viewModel = scheduleViewModel()

        viewModel.onDateSelected(LocalDate.of(2026, 9, 18))
        viewModel.onTimeSelected(LocalTime.of(10, 0))
        viewModel.onSubmit()

        assertEquals(ScheduleMomentError.NOT_IN_FUTURE, viewModel.state.value.momentError)
        assertFalse(viewModel.state.value.failed)
    }

    @Test
    fun `PR17 a patient with a visit already on the agenda is shown on the patient field, a network error is PR17_2`() {
        agenda.scheduleResult = failureOf(DomainError.Conflict("PatientAlreadyHasActiveScheduledFollowUp"))
        val viewModel = scheduleViewModel()
        viewModel.onDateSelected(LocalDate.of(2026, 9, 18))
        viewModel.onTimeSelected(LocalTime.of(10, 0))

        viewModel.onSubmit()
        assertEquals(SchedulePatientError.ALREADY_SCHEDULED, viewModel.state.value.patientError)

        agenda.scheduleResult = failureOf(DomainError.Network)
        viewModel.onSubmit()
        assertTrue(viewModel.state.value.failed)

        viewModel.onBackToForm()
        assertFalse(viewModel.state.value.failed)
        assertEquals(LocalDate.of(2026, 9, 18), viewModel.state.value.date)
    }

    @Test
    fun `PR17 from the agenda needs a patient chosen from the active roster`() {
        careLinks.rosterResult = Result.success(PatientRoster(listOf(rosterPatient(PATIENT_ID, "Ana Flores"))))
        val viewModel = scheduleViewModel(args = emptyMap())

        assertFalse(viewModel.state.value.patientFixed)
        assertEquals(listOf(PatientOption(PATIENT_ID, "Ana Flores")), viewModel.state.value.patients)
        viewModel.onDateSelected(LocalDate.of(2026, 9, 18))
        viewModel.onTimeSelected(LocalTime.of(10, 0))
        viewModel.onSubmit()
        assertEquals(SchedulePatientError.MISSING, viewModel.state.value.patientError)

        viewModel.onPatientSelected(PatientOption(PATIENT_ID, "Ana Flores"))
        viewModel.onSubmit()
        assertEquals(1, agenda.scheduled.size)
    }

    @Test
    fun `PR17 rescheduling is prefilled and calls rescheduling`() = runTest {
        val viewModel = scheduleViewModel(
            args = mapOf(
                ScheduleFollowUpRoute.ARG_PATIENT_ID to PATIENT_ID,
                ScheduleFollowUpRoute.ARG_PATIENT_NAME to "Ana Flores",
                ScheduleFollowUpRoute.ARG_FOLLOW_UP_ID to 31L,
                ScheduleFollowUpRoute.ARG_SCHEDULED_FOR to Instant.parse("2026-09-18T15:00:00Z").epochSecond,
                ScheduleFollowUpRoute.ARG_PREPARATION to "Fasting,EmptyBladder",
            ),
        )
        assertTrue(viewModel.state.value.isRescheduling)
        assertEquals(LocalDate.of(2026, 9, 18), viewModel.state.value.date)
        assertEquals(LocalTime.of(10, 0), viewModel.state.value.time)
        assertEquals(setOf(PreparationCode.FASTING, PreparationCode.EMPTY_BLADDER), viewModel.state.value.preparation)

        viewModel.events.test {
            viewModel.onTimeSelected(LocalTime.of(11, 30))
            viewModel.onSubmit()
            assertEquals(ScheduleFollowUpEvent.Scheduled(Instant.parse("2026-09-18T16:30:00Z"), rescheduled = true), awaitItem())
        }
        assertEquals(31L, agenda.rescheduled.single().first)
        assertTrue(agenda.scheduled.isEmpty())
    }

    // ----- PR17.0 -----

    private fun agendaViewModel() = PractitionerAgendaViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        getUpcomingFollowUps = GetUpcomingFollowUpsUseCase(agenda, clock),
        cancelFollowUp = CancelFollowUpUseCase(agenda),
        connectivityObserver = connectivity,
    )

    @Test
    fun `PR17_0 lists the scheduled visits from now, the closest first`() {
        agenda.agendaResult = Result.success(
            listOf(agendaVisit(id = 2, scheduledFor = Instant.parse("2026-09-22T21:30:00Z")), agendaVisit(id = 1)),
        )

        val viewModel = agendaViewModel()

        assertEquals(listOf(1L, 2L), viewModel.state.value.visits?.map { it.followUpId })
        assertEquals(FollowUpState.SCHEDULED to CONSULTATIONS_NOW, agenda.agendaRequests.single())
    }

    @Test
    fun `PR17_0 cancelling asks first and then removes the visit`() = runTest {
        agenda.agendaResult = Result.success(listOf(agendaVisit(id = 1)))
        val viewModel = agendaViewModel()
        val row = viewModel.state.value.visits!!.single()

        viewModel.events.test {
            viewModel.onVisitSelected(row)
            viewModel.onCancelRequested()
            assertEquals(AgendaDialog.CONFIRM_CANCEL, viewModel.state.value.dialog)
            agenda.agendaResult = Result.success(emptyList())
            viewModel.onCancelConfirmed()
            assertEquals(AgendaEvent.Cancelled, awaitItem())
        }
        assertEquals(listOf(1L), agenda.cancelled)
        assertNull(viewModel.state.value.dialog)
        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `PR17_0 a visit that is no longer scheduled cannot be cancelled`() {
        agenda.agendaResult = Result.success(listOf(agendaVisit(id = 1)))
        agenda.cancelResult = failureOf(DomainError.Conflict("FollowUpNotScheduled"))
        val viewModel = agendaViewModel()

        viewModel.onVisitSelected(viewModel.state.value.visits!!.single())
        viewModel.onCancelConfirmed()

        assertEquals(AgendaDialog.NOT_CANCELLABLE, viewModel.state.value.dialog)
    }

    @Test
    fun `a short cancellation reason is trimmed to 30 characters`() {
        assertNull(CancellationReason.ofOptional("  "))
        assertEquals(30, CancellationReason.ofOptional("x".repeat(40))!!.text.length)
    }

    // ----- PR16 -----

    @Test
    fun `PR16 E both fields are required`() {
        val invalid = ReferralValidation.of(PatientId(PATIENT_ID), " ", "")
        assertEquals(setOf(ReferralField.SPECIALTY, ReferralField.REASON), (invalid as ReferralValidation.Invalid).missing)

        val valid = ReferralValidation.of(PatientId(PATIENT_ID), " Endocrinología ", "Hipotiroidismo") as ReferralValidation.Valid
        assertEquals("Endocrinología", valid.referral.specialty.text)
    }

    private fun referralViewModel() = RecordReferralViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RecordReferralRoute.ARG_PATIENT_ID to PATIENT_ID)),
        recordReferral = RecordReferralUseCase(agenda),
        connectivityObserver = connectivity,
    )

    @Test
    fun `PR16 records and returns the specialty for the snackbar, a server error is PR16_2`() = runTest {
        val viewModel = referralViewModel()
        viewModel.onSubmit()
        assertEquals(setOf(ReferralField.SPECIALTY, ReferralField.REASON), viewModel.state.value.missing)

        agenda.referralResult = failureOf(DomainError.Unexpected("InternalError"))
        viewModel.onSpecialtyChange("Endocrinología")
        viewModel.onReasonChange("Hipotiroidismo sin control")
        viewModel.onSubmit()
        assertTrue(viewModel.state.value.failed)

        viewModel.onBackToForm()
        agenda.referralResult = Result.success(Unit)
        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(RecordReferralEvent.Recorded("Endocrinología"), awaitItem())
        }
        assertEquals("Hipotiroidismo sin control", agenda.referrals.last().reason.text)
    }

    @Test
    fun `PR16 without an active link says so`() {
        agenda.referralResult = failureOf(DomainError.Forbidden("ActiveCareLinkRequired"))
        val viewModel = referralViewModel()
        viewModel.onSpecialtyChange("Endocrinología")
        viewModel.onReasonChange("Motivo")

        viewModel.onSubmit()

        assertTrue(viewModel.state.value.noActiveLink)
    }

    // ----- Mappers -----

    @Test
    fun `the agenda resource is read with its name and preparation, an unknown state is skipped`() {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
        val dto = json.decodeFromString(
            ScheduledFollowUpDto.serializer(),
            """{"followUpId":31,"patientId":7,"practitionerId":3,"scheduledFor":"2026-09-18T10:00:00-05:00",
               "state":"Scheduled","patientFullName":"Ana Flores","preparation":["Fasting","BringOwnScale"],"modality":"Remote"}""",
        )

        val visit = dto.toDomainOrNull()!!
        assertEquals(Instant.parse("2026-09-18T15:00:00Z"), visit.scheduledFor)
        assertEquals(
            listOf(PreparationInstruction.Catalog(PreparationCode.FASTING), PreparationInstruction.Custom("BringOwnScale")),
            visit.preparation,
        )
        assertEquals(ConsultationModality.REMOTE, visit.modality)
        assertTrue(visit.canBeChanged)
        assertNull(dto.copy(state = "Archived").toDomainOrNull())
    }

    @Test
    fun `a new visit is sent in UTC with the preparation codes`() {
        val dto = NewFollowUp(
            PatientId(PATIENT_ID),
            Instant.parse("2026-09-18T15:00:00Z"),
            FollowUpPreparation(setOf(PreparationCode.EMPTY_BLADDER, PreparationCode.FASTING)),
        ).toDto()

        assertEquals(
            ScheduleFollowUpRequestDto(PATIENT_ID, "2026-09-18T15:00:00Z", listOf("Fasting", "EmptyBladder"), "InPerson"),
            dto,
        )
    }
}
