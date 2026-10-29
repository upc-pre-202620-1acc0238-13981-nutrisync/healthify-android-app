package pe.edu.upc.healthify.features.foodcatalog.presentation.state

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodField
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood

/** A catalog row: "Cuy al horno (local)" · "176 kcal / 100 g". */
data class FoodRow(val id: Long, val name: String, val energyKcalPer100g: Double, val isLocal: Boolean) {
    companion object {
        fun of(food: ReferenceFood) = FoodRow(food.id.value, food.name, food.energyKcalPer100g, food.isLocalOverride)
    }
}

/**
 * Food catalog. Catalog management is not offline (the patient's search is): offline and
 * without a previous read the offline state shows. `results = null` = not read yet.
 */
data class FoodCatalogUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val results: List<FoodRow>? = null,
) {
    val isEmpty: Boolean get() = results?.isEmpty() == true
    val needsConnection: Boolean get() = results == null && loadFailed && isOffline
}

/** Add local food. */
data class AddLocalFoodUiState(
    val name: String = "",
    val energy: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val invalidFields: Set<LocalFoodField> = emptySet(),
    /** `409 DuplicatedLocalOverride`: "A local food with this name already exists". */
    val duplicatedName: Boolean = false,
    val isSaving: Boolean = false,
    val showServerError: Boolean = false,
    val isOffline: Boolean = false,
)
