package pe.edu.upc.healthify.features.intake.infrastructure.sync

import kotlinx.serialization.Serializable

/**
 * What Intake stores in the generic offline queue (`pending_operations.payload`) for a meal. It carries what
 * `PendingDiaryEntryResource` asks for plus [foodName], only to show it in the pending lists (it is not sent).
 *
 * [localTimestamp] is the exact ISO-8601 text the patient declared: it is resent untouched (the backend rejects
 * a resend that differs with `LocalTimestampCannotBeRewritten`).
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
        /** The queue `type` for diary meals ([DiaryEntrySyncSender] sends them). */
        const val TYPE = "intake.diary-entry"
    }
}
