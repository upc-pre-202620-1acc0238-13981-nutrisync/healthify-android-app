package pe.edu.upc.healthify.features.intake.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Photo logging in a single route, from camera to confirmation (the steps are states of one
 * ViewModel). With [pendingPhotoId] it starts by analyzing a photo taken offline.
 */
@Serializable
data class MealPhotoRoute(val pendingPhotoId: String? = null) {
    companion object {
        const val ARG_PENDING_PHOTO_ID = "pendingPhotoId"
    }
}

/**
 * Logging by hand. With [pickFoodOnly] it only picks a food and returns it
 * to the previous screen ("Not this dish? Tap to change it.").
 */
@Serializable
data class ManualMealRoute(val pickFoodOnly: Boolean = false) {
    companion object {
        const val ARG_PICK_FOOD_ONLY = "pickFoodOnly"
    }
}

/** Confirming a diary entry left "To confirm": the entry is read again from day [date] (`2026-09-09`). */
@Serializable
data class ConfirmEntryRoute(val diaryEntryId: Long, val date: String) {
    companion object {
        const val ARG_DIARY_ENTRY_ID = "diaryEntryId"
        const val ARG_DATE = "date"
    }
}

/** Ideas for today → idea detail (two states of the same route). */
@Serializable
data object MealIdeasRoute

/** Self weigh-in (opened from "Log self weigh-in" on the trend screen). */
@Serializable
data object SelfWeighInRoute

/** Pending to sync. */
@Serializable
data object PendingSyncRoute

/** Result of [ManualMealRoute] with `pickFoodOnly`: left in the previous screen's `SavedStateHandle`. */
object PickedFoodResult {
    const val KEY_ID = "picked_food_id"
    const val KEY_NAME = "picked_food_name"
}

/** Reminders (and the permission dialog), opened from the menu. */
@Serializable
data object RemindersRoute
