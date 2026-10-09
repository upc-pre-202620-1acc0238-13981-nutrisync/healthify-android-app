package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Estado de una consulta agendada (`ScheduledFollowUpResource.state`, MA-2). Una no acudida no cierra el vínculo. */
enum class FollowUpState(val code: String) {
    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    MISSED("Missed"),
    CANCELLED("Cancelled"),
    ;

    companion object {
        fun fromCode(code: String?): FollowUpState? = entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/** Una consulta de la agenda del nutricionista (read model *Practitioner Agenda*, MA-2). */
data class AgendaVisit(
    val id: Long,
    val patientId: PatientId,
    val patientFullName: String?,
    val scheduledFor: Instant,
    val state: FollowUpState,
    val preparation: List<PreparationInstruction>,
    val modality: ConsultationModality,
) {
    init {
        require(id > 0) { "A follow up id is positive" }
    }

    /** Solo una consulta agendada se cancela o reprograma (MA-5). */
    val canBeChanged: Boolean get() = state == FollowUpState.SCHEDULED
}

/** Indicaciones de «¿Cómo debe prepararse?» (PR17): lista cerrada, varias a la vez. */
@JvmInline
value class FollowUpPreparation(val codes: Set<PreparationCode>) {
    /** En el orden del catálogo, como las espera el backend. */
    val orderedCodes: List<PreparationCode> get() = PreparationCode.entries.filter { it in codes }
}

/** Una consulta nueva (`ScheduleFollowUpResource`): el momento ya pasó por [FollowUpMoment.of]. */
data class NewFollowUp(
    val patientId: PatientId,
    val scheduledFor: Instant,
    val preparation: FollowUpPreparation,
    val modality: ConsultationModality = ConsultationModality.IN_PERSON,
)

/**
 * Fecha y hora de PR17 → el momento de la consulta. **Debe estar en el futuro** (MA-5:
 * `400 ScheduledForMustBeInFuture`): se valida en el teléfono antes de enviar (PR17.E «Elige una fecha y hora
 * futuras»), y el backend lo vuelve a validar.
 */
sealed interface FollowUpMoment {
    data class Valid(val instant: Instant) : FollowUpMoment

    /** Falta la fecha o la hora. */
    data object Incomplete : FollowUpMoment

    /** La fecha y hora elegidas ya pasaron (o son ahora mismo). */
    data object NotInFuture : FollowUpMoment

    companion object {
        fun of(date: LocalDate?, time: LocalTime?, zone: ZoneId, now: Instant): FollowUpMoment {
            if (date == null || time == null) return Incomplete
            val instant = date.atTime(time).atZone(zone).toInstant()
            return if (instant.isAfter(now)) Valid(instant) else NotInFuture
        }
    }
}

/** Motivo opcional de una cancelación (`CancelFollowUpResource.reason`, ≤ 30 caracteres, MA-5). */
@JvmInline
value class CancellationReason private constructor(val text: String) {
    companion object {
        const val MAX_LENGTH = 30

        /** `null` si viene vacío; recorta a [MAX_LENGTH] (el backend responde `400` si se pasa). */
        fun ofOptional(text: String?): CancellationReason? =
            text?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_LENGTH)?.let(::CancellationReason)
    }
}
