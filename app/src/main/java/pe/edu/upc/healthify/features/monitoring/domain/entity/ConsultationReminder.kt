package pe.edu.upc.healthify.features.monitoring.domain.entity

import java.time.Duration
import java.time.Instant

/**
 * Aviso local de una consulta agendada, para el paciente y para su nutricionista: un día antes y una hora antes. Solo
 * lleva la hora y, para el nutricionista, el nombre del paciente; nunca datos clínicos.
 *
 * @param forPractitioner el aviso es para el nutricionista (si no, para el paciente).
 * @param patientFullName el paciente de la consulta (solo en los avisos del nutricionista).
 */
data class ConsultationReminder(
    val followUpId: Long,
    val scheduledFor: Instant,
    val forPractitioner: Boolean = false,
    val patientFullName: String? = null,
) {
    /** Los avisos que todavía no pasaron, el más próximo primero. */
    fun triggersAfter(now: Instant): List<ReminderTrigger> =
        LEADS.map { ReminderTrigger(it, scheduledFor.minus(it)) }
            .filter { it.at.isAfter(now) }
            .sortedBy { it.at }

    companion object {
        /** Con cuánta anticipación se avisa. */
        val LEADS: List<Duration> = listOf(Duration.ofHours(24), Duration.ofHours(1))
    }
}

/** Un aviso de [ConsultationReminder]: [lead] antes de la consulta, a las [at]. */
data class ReminderTrigger(val lead: Duration, val at: Instant)
