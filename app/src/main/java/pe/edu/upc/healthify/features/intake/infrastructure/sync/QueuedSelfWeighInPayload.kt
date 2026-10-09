package pe.edu.upc.healthify.features.intake.infrastructure.sync

import kotlinx.serialization.Serializable

/**
 * Lo que Intake guarda en la cola offline genérica (`pending_operations.payload`) para un autopesaje: lo que pide
 * `PendingSelfWeighInResource` (IN-4). [localTimestamp] es el texto ISO-8601 exacto que declaró el paciente y se
 * reenvía sin tocar.
 */
@Serializable
data class QueuedSelfWeighInPayload(
    val clientEntryId: String,
    val valueKg: Double,
    val localTimestamp: String,
    val fastedState: Boolean,
) {
    companion object {
        /** El `type` de la cola para los autopesajes (lo envía [SelfWeighInSyncSender]). */
        const val TYPE = "intake.self-weigh-in"
    }
}
