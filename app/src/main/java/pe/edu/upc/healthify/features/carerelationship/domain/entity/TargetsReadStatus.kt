package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import java.time.Instant

/**
 * `TargetsReadStatusResource` (F12, CR-3): si el paciente **vio** las metas publicadas. No dice nada de si las
 * sigue. La marca vive en el vínculo, no en el plan.
 *
 * @param pendingVersion versión publicada que espera el acuse («Tus metas cambiaron (versión 4)»), o `null`.
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

    /** La versión que el paciente todavía no revisó (PT3.M), o `null` si no hay nada pendiente. */
    val versionAwaitingReview: Int? get() = pendingVersion?.takeIf { hasPendingAcknowledgement }
}
