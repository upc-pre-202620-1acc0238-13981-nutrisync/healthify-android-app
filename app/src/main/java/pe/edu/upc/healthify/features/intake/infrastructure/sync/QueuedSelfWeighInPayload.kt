package pe.edu.upc.healthify.features.intake.infrastructure.sync

import kotlinx.serialization.Serializable

/**
 * What Intake stores in the generic offline queue (`pending_operations.payload`) for a self weigh-in: what
 * `PendingSelfWeighInResource` asks for. [localTimestamp] is the exact ISO-8601 text the patient declared and is
 * resent untouched.
 */
@Serializable
data class QueuedSelfWeighInPayload(
    val clientEntryId: String,
    val valueKg: Double,
    val localTimestamp: String,
    val fastedState: Boolean,
) {
    companion object {
        /** The queue `type` for self weigh-ins ([SelfWeighInSyncSender] sends them). */
        const val TYPE = "intake.self-weigh-in"
    }
}
