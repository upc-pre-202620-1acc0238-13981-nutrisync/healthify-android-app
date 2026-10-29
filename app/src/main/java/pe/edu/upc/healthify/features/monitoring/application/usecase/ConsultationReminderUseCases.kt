package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationReminder
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationReminderScheduler
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerAgendaRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import javax.inject.Inject

/**
 * Re-reads the user's upcoming visits and reschedules their reminders: the patient's next visit or the nutritionist's
 * visits scheduled from now on. If the read fails, the existing reminders stay as they were.
 */
class RefreshConsultationRemindersUseCase @Inject constructor(
    private val patientRepository: PatientMonitoringRepository,
    private val agendaRepository: PractitionerAgendaRepository,
    private val scheduler: ConsultationReminderScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(userId: Long, isPractitioner: Boolean): Result<Unit> {
        val reminders = if (isPractitioner) {
            agendaRepository.getAgenda(userId, FollowUpState.SCHEDULED, clock.instant()).map { visits ->
                visits.map { ConsultationReminder(it.id, it.scheduledFor, forPractitioner = true, patientFullName = it.patientFullName) }
            }
        } else {
            patientRepository.getNextFollowUp(PatientId(userId)).map { next ->
                listOfNotNull(next?.let { ConsultationReminder(it.id.value, it.scheduledFor) })
            }
        }
        return reminders.map { scheduler.replaceAll(userId, it) }
    }
}

/** On app start or after the agenda changes: asks to reschedule the reminders in the background. */
class RequestConsultationRemindersUseCase @Inject constructor(
    private val scheduler: ConsultationReminderScheduler,
) {
    operator fun invoke() = scheduler.requestRefresh()
}
