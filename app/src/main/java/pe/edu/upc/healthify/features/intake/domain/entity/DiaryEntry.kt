package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import java.time.Instant
import java.time.LocalDate

/**
 * A diary entry (`DiaryEntryResource`). **It is never deleted** (*Diary Entry Never Deleted*): there is no way to
 * remove it, only to confirm its portion if it was left "To confirm".
 *
 * The AI proposal and what the patient confirmed are stored together (*Proposal Kept Alongside Confirmation*):
 * [proposedGrams] survives a different [confirmedGrams].
 *
 * @param confidence of the photo estimate, between 0 and 1 (*Confidence Always Attached*).
 * @param isCountedTowardsTargets only what is confirmed counts towards the day's targets.
 * @param mealGroupId entries logged together (a meal idea): the diary shows them as one meal.
 */
data class DiaryEntry(
    val id: DiaryEntryId,
    val localTimestamp: LocalTimestamp,
    val provenance: EntryProvenance,
    val foodName: String?,
    val proposedFoodId: Long?,
    val proposedGrams: Double?,
    val confidence: Double?,
    val confirmedFoodId: Long?,
    val confirmedGrams: Double?,
    val planAdherence: PlanAdherence,
    val isCountedTowardsTargets: Boolean,
    val mealGroupId: String? = null,
    val fromMealIdea: Boolean = false,
) {
    init {
        require(confidence == null || confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        require(proposedGrams == null || proposedGrams > 0) { "A proposed portion must be positive" }
        require(confirmedGrams == null || confirmedGrams > 0) { "A confirmed portion must be positive" }
    }

    /** A photo proposal not confirmed yet: "To confirm · Does not count towards your targets yet". */
    val isPendingConfirmation: Boolean
        get() = provenance == EntryProvenance.PHOTO && confirmedGrams == null && proposedGrams != null

    /** The confirmed amount if there is one; otherwise the proposal ("280 g approx."). */
    val displayedGrams: Double? get() = confirmedGrams ?: proposedGrams

    val isOffPlan: Boolean get() = planAdherence == PlanAdherence.OFF_PLAN || provenance == EntryProvenance.OFF_PLAN
}

/**
 * One diary day.
 *
 * @param savedAt when the phone's copy was saved; only if [fromCache] (offline).
 */
data class DiaryDay(
    val date: LocalDate,
    val entries: List<DiaryEntry>,
    val fromCache: Boolean = false,
    val savedAt: Instant? = null,
) {
    init {
        require(!fromCache || savedAt != null) { "A cached day needs savedAt" }
    }

    /** "To confirm" first (they ask for an action); then from newest to oldest. */
    val orderedEntries: List<DiaryEntry>
        get() = entries.sortedWith(
            compareByDescending<DiaryEntry> { it.isPendingConfirmation }.thenByDescending { it.localTimestamp.instant },
        )
}
