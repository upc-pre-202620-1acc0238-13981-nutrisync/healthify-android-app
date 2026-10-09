package pe.edu.upc.healthify.features.foodcatalog.presentation.state

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodField
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood

/** Una fila de PR15: «Cuy al horno (local)» · «176 kcal / 100 g». */
data class FoodRow(val id: Long, val name: String, val energyKcalPer100g: Double, val isLocal: Boolean) {
    companion object {
        fun of(food: ReferenceFood) = FoodRow(food.id.value, food.name, food.energyKcalPer100g, food.isLocalOverride)
    }
}

/**
 * PR15 · Catálogo de alimentos. La gestión del catálogo no es offline (la búsqueda del paciente sí): sin conexión y
 * sin una lectura previa se muestra el estado offline. `results = null` = todavía no se leyó.
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

/** PR15.1 · Agregar alimento local. */
data class AddLocalFoodUiState(
    val name: String = "",
    val energy: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val invalidFields: Set<LocalFoodField> = emptySet(),
    /** `409 DuplicatedLocalOverride`: «Ya existe un alimento local con este nombre». */
    val duplicatedName: Boolean = false,
    val isSaving: Boolean = false,
    val showServerError: Boolean = false,
    val isOffline: Boolean = false,
)
