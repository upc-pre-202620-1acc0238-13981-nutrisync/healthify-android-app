package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `TargetsReadStatusResource` (F12, CR-3). */
@Serializable
data class TargetsReadStatusDto(
    val careLinkId: Long,
    val patientId: Long,
    val pendingTargetsVersion: Int? = null,
    val lastAcknowledgedVersion: Int? = null,
    val hasPendingAcknowledgement: Boolean = false,
    val lastAcknowledgedAt: String? = null,
)

/** `AcknowledgeActiveTargetsResource(PlanVersion)` (F12). */
@Serializable
data class AcknowledgeActiveTargetsRequestDto(
    val planVersion: Int,
)
