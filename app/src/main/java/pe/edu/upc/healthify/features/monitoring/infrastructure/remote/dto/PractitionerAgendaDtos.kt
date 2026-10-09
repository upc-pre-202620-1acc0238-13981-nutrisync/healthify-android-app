package pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `ScheduledFollowUpResource` (read model *Practitioner Agenda*, MA-2). */
@Serializable
data class ScheduledFollowUpDto(
    val followUpId: Long,
    val patientId: Long,
    val practitionerId: Long,
    val scheduledFor: String,
    val state: String,
    val missedAt: String? = null,
    val patientFullName: String? = null,
    val preparation: List<String>? = null,
    val modality: String? = null,
    val scheduledAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
)

/** `ScheduleFollowUpResource`. */
@Serializable
data class ScheduleFollowUpRequestDto(
    val patientId: Long,
    val scheduledFor: String,
    val preparation: List<String>,
    val modality: String,
)

/** `RescheduleFollowUpResource`: la preparación enviada reemplaza a la anterior. */
@Serializable
data class RescheduleFollowUpRequestDto(val scheduledFor: String, val preparation: List<String>)

/** `CancelFollowUpResource`. */
@Serializable
data class CancelFollowUpRequestDto(val reason: String? = null)

/** `RecordReferralResource`. */
@Serializable
data class RecordReferralRequestDto(val patientId: Long, val specialty: String, val reason: String)
