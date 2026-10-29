package pe.edu.upc.healthify.features.monitoring

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.monitoring.application.usecase.RefreshConsultationRemindersUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeConsultationReminderScheduler
import pe.edu.upc.healthify.testing.FakePatientMonitoringRepository
import pe.edu.upc.healthify.testing.FakePractitionerAgendaRepository
import pe.edu.upc.healthify.testing.agendaVisit
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import java.time.Duration

class ConsultationRemindersTest {

    private val patients = FakePatientMonitoringRepository()
    private val agenda = FakePractitionerAgendaRepository()
    private val scheduler = FakeConsultationReminderScheduler()
    private val refresh = RefreshConsultationRemindersUseCase(patients, agenda, scheduler, fixedClock())

    @Test
    fun `a consultation is announced a day and an hour before`() {
        val reminder = ConsultationReminder(7, CONSULTATIONS_NOW.plus(Duration.ofDays(3)))

        val triggers = reminder.triggersAfter(CONSULTATIONS_NOW)

        assertEquals(listOf(Duration.ofHours(24), Duration.ofHours(1)), triggers.map { it.lead })
        assertEquals(reminder.scheduledFor.minus(Duration.ofHours(24)), triggers.first().at)
    }

    @Test
    fun `notices already in the past are skipped`() {
        val reminder = ConsultationReminder(7, CONSULTATIONS_NOW.plus(Duration.ofHours(5)))

        assertEquals(listOf(Duration.ofHours(1)), reminder.triggersAfter(CONSULTATIONS_NOW).map { it.lead })
    }

    @Test
    fun `the patient gets the notices of the next consultation`() = runTest {
        val scheduled = CONSULTATIONS_NOW.plus(Duration.ofDays(2))
        patients.nextFollowUpResult = Result.success(NextFollowUp(FollowUpId(7), scheduled))

        refresh(userId = 5, isPractitioner = false)

        assertEquals(listOf(5L to listOf(ConsultationReminder(7, scheduled))), scheduler.replaced)
    }

    @Test
    fun `the practitioner gets one notice per scheduled consultation with the patient name`() = runTest {
        agenda.agendaResult = Result.success(listOf(agendaVisit(id = 31), agendaVisit(id = 32)))

        refresh(userId = 9, isPractitioner = true)

        val (userId, reminders) = scheduler.replaced.single()
        assertEquals(9L, userId)
        assertEquals(listOf(31L, 32L), reminders.map { it.followUpId })
        assertTrue(reminders.all { it.forPractitioner && it.patientFullName == "Ana Flores" })
    }

    @Test
    fun `without network the notices already scheduled stay as they were`() = runTest {
        patients.nextFollowUpResult = failureOf(DomainError.Network)

        refresh(userId = 5, isPractitioner = false)

        assertTrue(scheduler.replaced.isEmpty())
    }
}
