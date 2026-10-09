package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence

/**
 * Una comida registrada sin conexión que espera en la cola del teléfono (PT14.O «Pendiente de enviar», PT19). Se
 * reenvía siempre con el mismo [clientEntryId] y el mismo [localTimestamp] (nunca se reescribe la hora local).
 *
 * @param confirmed `false` = foto «Por confirmar» guardada al salir de PT7 sin confirmar.
 * @param rejectionCode el backend no la aceptó al sincronizar (`LocalTimestampCannotBeRewritten`,
 *   `ReferenceFoodNotResolved`…): queda a la vista y no se reintenta.
 */
data class PendingDiaryEntry(
    val clientEntryId: ClientEntryId,
    val localTimestamp: LocalTimestamp,
    val provenance: EntryProvenance,
    val foodName: String,
    val grams: Double,
    val confidence: Double?,
    val confirmed: Boolean,
    val planAdherence: PlanAdherence,
    val rejectionCode: String? = null,
) {
    init {
        require(foodName.isNotBlank()) { "A pending entry keeps its food name to be shown" }
        require(grams > 0) { "A pending entry has a portion" }
        require(confidence == null || confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
    }

    val isRejected: Boolean get() = rejectionCode != null
}
