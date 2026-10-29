package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant

/**
 * The patient's next visit (`PatientFollowUpResource`; `UpcomingConsultationResource`). Read-only: the nutritionist
 * schedules or moves it. It exposes no clinical notes or diagnosis.
 *
 * @param scheduledAt when the nutritionist scheduled it ("Scheduled on September 4"), if the backend knows.
 * @param practitionerFullName the nutritionist's name as is (a person's text).
 */
data class NextFollowUp(
    val id: FollowUpId,
    val scheduledFor: Instant,
    val modality: ConsultationModality = ConsultationModality.IN_PERSON,
    val preparation: List<PreparationInstruction> = emptyList(),
    val scheduledAt: Instant? = null,
    val practitionerFullName: String? = null,
) {
    /**
     * "Check In Editable Until The Visit": the check-in can only be sent or edited before the visit's time.
     * The backend decides (`409 CheckInLocked`); this avoids offering what is no longer possible.
     */
    fun acceptsCheckInAt(now: Instant): Boolean = now.isBefore(scheduledFor)
}

/** Id of a scheduled visit (`followUpId`). */
@JvmInline
value class FollowUpId(val value: Long) {
    init {
        require(value > 0) { "FollowUpId must be positive" }
    }
}
