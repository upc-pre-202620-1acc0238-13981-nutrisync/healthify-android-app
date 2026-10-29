package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** State of a scheduled visit (`ScheduledFollowUpResource.state`). A missed visit does not close the care link. */
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

/** A visit on the nutritionist's agenda (read model *Practitioner Agenda*). */
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

    /** Only a scheduled visit can be cancelled or rescheduled. */
    val canBeChanged: Boolean get() = state == FollowUpState.SCHEDULED
}

/** "How should they prepare?" instructions: a closed list, several at once. */
@JvmInline
value class FollowUpPreparation(val codes: Set<PreparationCode>) {
    /** In catalog order, as the backend expects them. */
    val orderedCodes: List<PreparationCode> get() = PreparationCode.entries.filter { it in codes }
}

/** A new visit (`ScheduleFollowUpResource`): the moment already went through [FollowUpMoment.of]. */
data class NewFollowUp(
    val patientId: PatientId,
    val scheduledFor: Instant,
    val preparation: FollowUpPreparation,
    val modality: ConsultationModality = ConsultationModality.IN_PERSON,
)

/**
 * Date and time from the schedule form -> the visit moment. **Must be in the future**
 * (`400 ScheduledForMustBeInFuture`): checked on the phone before sending ("Pick a future date and time"), and the
 * backend checks it again.
 */
sealed interface FollowUpMoment {
    data class Valid(val instant: Instant) : FollowUpMoment

    /** Date or time missing. */
    data object Incomplete : FollowUpMoment

    /** The chosen date and time already passed (or are right now). */
    data object NotInFuture : FollowUpMoment

    companion object {
        fun of(date: LocalDate?, time: LocalTime?, zone: ZoneId, now: Instant): FollowUpMoment {
            if (date == null || time == null) return Incomplete
            val instant = date.atTime(time).atZone(zone).toInstant()
            return if (instant.isAfter(now)) Valid(instant) else NotInFuture
        }
    }
}

/** Optional cancellation reason (`CancelFollowUpResource.reason`, ≤ 30 characters). */
@JvmInline
value class CancellationReason private constructor(val text: String) {
    companion object {
        const val MAX_LENGTH = 30

        /** `null` if empty; trims to [MAX_LENGTH] (the backend answers `400` if it is longer). */
        fun ofOptional(text: String?): CancellationReason? =
            text?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_LENGTH)?.let(::CancellationReason)
    }
}
