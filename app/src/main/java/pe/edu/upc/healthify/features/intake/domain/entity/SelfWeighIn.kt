package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg

/**
 * A new self weigh-in. The only protocol question asked is "Did you weigh yourself fasting?"; a non-fasting
 * reading is stored in full and only does not smooth the trend (nothing is rejected or flagged).
 *
 * [clientEntryId] is generated once per self weigh-in: if it stays in the offline queue, it is always resent the
 * same way to `…/synchronization` and the backend does not duplicate it.
 */
data class NewSelfWeighIn(
    val weight: WeightKg,
    val localTimestamp: LocalTimestamp,
    val fasted: Boolean,
    val clientEntryId: ClientEntryId,
)

/** What happened on save: it reached the backend or stayed on the phone until there is a connection. */
enum class SelfWeighInOutcome { RECORDED, QUEUED }

/**
 * A self weigh-in waiting in the phone's queue. The time is shown as it was saved.
 *
 * @param rejectionCode the backend did not accept it on sync (`ImplausibleWeightValue`…): it stays visible and is
 *   not retried.
 */
data class PendingSelfWeighIn(
    val clientEntryId: ClientEntryId,
    val weight: WeightKg,
    val localTimestamp: LocalTimestamp,
    val fasted: Boolean,
    val rejectionCode: String? = null,
) {
    val isRejected: Boolean get() = rejectionCode != null
}
