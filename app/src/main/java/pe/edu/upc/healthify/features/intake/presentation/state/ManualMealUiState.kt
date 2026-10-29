package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import java.time.LocalDate

/**
 * Food search (and keyboard), not found, found and could not log.
 *
 * @param searchedTerm the term behind [results] (`null` = nothing searched yet: "We could not find it" is not shown).
 * @param localOnly the server did not answer: "You are offline: we only searched your saved catalog.".
 * @param foodError "Pick a food from the list." or "We could not find this food…" (backend 422).
 */
data class ManualMealUiState(
    val today: LocalDate,
    /** The day the account was created: the day picker does not offer earlier days. */
    val earliestDay: LocalDate? = null,
    val form: MealFormState,
    val pickFoodOnly: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<ReferenceFood> = emptyList(),
    val searchedTerm: String? = null,
    val localOnly: Boolean = false,
    val selectedFood: ReferenceFood? = null,
    val foodError: FoodError? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isOffline: Boolean = false,
) {
    /** The search's empty state. */
    val notFound: Boolean get() = searchedTerm != null && !isSearching && results.isEmpty()

    enum class FoodError { NOT_CHOSEN, NOT_RESOLVED }
}

sealed interface ManualMealEvent {
    data class Logged(val outcome: MealLogOutcome) : ManualMealEvent

    /** "Pick a food" mode (from the photo confirmation): returns with the picked food. */
    data class FoodPicked(val referenceFoodId: Long, val name: String) : ManualMealEvent
}
