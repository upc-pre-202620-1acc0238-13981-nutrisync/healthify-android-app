package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import java.time.Instant

/**
 * `TargetsReadStatusResource`: whether the patient **saw** the published targets. It says nothing about whether
 * they follow them. The mark lives on the care link, not on the plan.
 *
 * @param pendingVersion published version awaiting acknowledgement ("Your targets changed (version 4)"), or `null`.
 */
data class TargetsReadStatus(
    val careLinkId: CareLinkId,
    val pendingVersion: Int?,
    val lastAcknowledgedVersion: Int?,
    val hasPendingAcknowledgement: Boolean,
    val lastAcknowledgedAt: Instant?,
) {
    init {
        require(pendingVersion == null || pendingVersion > 0) { "Plan versions are positive" }
        require(lastAcknowledgedVersion == null || lastAcknowledgedVersion > 0) { "Plan versions are positive" }
    }

    /** The version the patient has not reviewed yet, or `null` if nothing is pending. */
    val versionAwaitingReview: Int? get() = pendingVersion?.takeIf { hasPendingAcknowledgement }
}
