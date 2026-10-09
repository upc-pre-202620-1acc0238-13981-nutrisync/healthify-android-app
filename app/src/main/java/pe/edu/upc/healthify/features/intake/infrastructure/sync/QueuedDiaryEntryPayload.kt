package pe.edu.upc.healthify.features.intake.infrastructure.sync

import kotlinx.serialization.Serializable

/**
 * Lo que Intake guarda en la cola offline genérica (`pending_operations.payload`) para una comida. Lleva lo que pide
 * `PendingDiaryEntryResource` más [foodName], solo para mostrarla en PT14.O y PT19 (no se envía).
 *
 * [localTimestamp] es el texto ISO-8601 exacto que declaró el paciente: se reenvía sin tocar (el backend rechaza
 * con `LocalTimestampCannotBeRewritten` un reenvío que discrepa).
 */
@Serializable
data class QueuedDiaryEntryPayload(
    val clientEntryId: String,
    val localTimestamp: String,
    val provenance: String,
    val referenceFoodId: Long,
    val portionGrams: Double,
    val foodName: String,
    val confidence: Double? = null,
    val confirmed: Boolean = true,
    val planAdherence: String? = null,
) {
    companion object {
        /** El `type` de la cola para las comidas del diario (lo envía [DiaryEntrySyncSender]). */
        const val TYPE = "intake.diary-entry"
    }
}
