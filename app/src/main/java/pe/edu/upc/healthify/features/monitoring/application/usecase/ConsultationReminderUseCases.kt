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
 * Relee las próximas consultas del usuario y reprograma sus avisos: la próxima del paciente (MA-3) o las agendadas del
 * nutricionista desde ahora (PR17.0). Si la lectura falla, los avisos que había siguen como estaban.
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

/** Al entrar a la app o tras cambiar la agenda: pide reprogramar los avisos en segundo plano. */
class RequestConsultationRemindersUseCase @Inject constructor(
    private val scheduler: ConsultationReminderScheduler,
) {
    operator fun invoke() = scheduler.requestRefresh()
}
