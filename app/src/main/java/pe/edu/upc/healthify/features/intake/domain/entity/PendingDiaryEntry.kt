package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence

/**
 * A meal logged offline that waits in the phone's queue ("Pending to send"). It is always resent with the
 * same [clientEntryId] and the same [localTimestamp] (the local time is never rewritten).
 *
 * @param confirmed `false` = a "To confirm" photo saved when leaving the photo result without confirming.
 * @param rejectionCode the backend did not accept it on sync (`LocalTimestampCannotBeRewritten`,
 *   `ReferenceFoodNotResolved`…): it stays visible and is not retried.
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
