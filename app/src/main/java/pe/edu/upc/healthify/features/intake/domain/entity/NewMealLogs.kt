package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams

/** A meal written by hand: it is a confirmation from the start. */
data class NewManualMeal(
    val food: MealFood,
    val portion: PortionGrams,
    val localTimestamp: LocalTimestamp,
    val planAdherence: PlanAdherence,
    val clientEntryId: ClientEntryId,
) {
    init {
        require(planAdherence.isAnswered) { "A manual meal needs the plan answer (PlanAdherenceRequired)" }
    }
}

/** What the patient did with the photo estimate. */
sealed interface PhotoConfirmation {
    /** "Yes, that's right". */
    data object AsProposed : PhotoConfirmation

    /** Another dish (an alternative or one from the catalog) and/or other grams. The proposal is kept alongside. */
    data class Adjusted(val food: MealFood, val portion: PortionGrams) : PhotoConfirmation
}

/** A photo meal already confirmed (`photo-logs` with `analysisId`). */
data class NewPhotoMeal(
    val analysis: MealPhotoAnalysis,
    val confirmation: PhotoConfirmation,
    val localTimestamp: LocalTimestamp,
    val planAdherence: PlanAdherence,
    val clientEntryId: ClientEntryId,
) {
    init {
        require(planAdherence.isAnswered) { "A confirmed photo meal needs the plan answer (PlanAdherenceRequired)" }
    }

    /** What gets logged: the confirmed dish and grams. */
    val loggedFood: MealFood
        get() = when (confirmation) {
            PhotoConfirmation.AsProposed -> analysis.food
            is PhotoConfirmation.Adjusted -> confirmation.food
        }

    val loggedGrams: Double
        get() = when (confirmation) {
            PhotoConfirmation.AsProposed -> analysis.estimatedGrams
            is PhotoConfirmation.Adjusted -> confirmation.portion.value
        }
}

/** One food of a multi-food meal (`manual-logs/batch`). */
data class MealGroupItem(val food: MealFood, val portion: PortionGrams, val clientEntryId: ClientEntryId)

/** "Log this meal": N `Manual` entries with the same moment, in a single transaction. */
data class NewMealGroup(
    val items: List<MealGroupItem>,
    val localTimestamp: LocalTimestamp,
    val mealIdeaId: String?,
    val description: String,
    val planAdherence: PlanAdherence = PlanAdherence.IN_PLAN,
) {
    init {
        require(items.size in 1..MAX_ITEMS) { "A meal group has 1 to $MAX_ITEMS foods (InvalidMealGroupItems)" }
        require(items.map { it.clientEntryId }.toSet().size == items.size) { "Each food has its own clientEntryId" }
        require(planAdherence.isAnswered) { "A meal group needs the plan answer" }
    }

    companion object {
        const val MAX_ITEMS = 10
    }
}

/** How a log ended. */
enum class MealLogOutcome {
    /** The backend saved it. */
    LOGGED,

    /** Offline: it stayed in the phone's queue and will be sent on its own ("Pending to send"). */
    QUEUED,
}

/**
 * Message for the diary Snackbar when returning to the diary: "Meal logged: Lomo saltado · 320 g" or
 * "Saved. It will be sent when you are online.".
 */
data class LoggedMealNotice(
    val description: String,
    val grams: Double?,
    val outcome: MealLogOutcome,
)
