package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import java.time.LocalDate

/**
 * PT9 Buscar alimento (+ PT9.K teclado), PT10 No encontrado, PT10.1 Encontrado y PT10.3 No se pudo registrar.
 *
 * @param searchedTerm el término de [results] (`null` = todavía no se buscó: no se muestra «No lo encontramos»).
 * @param localOnly el servidor no respondió: «Estás sin conexión: solo buscamos en tu catálogo guardado.».
 * @param foodError «Elige un alimento de la lista.» o «No encontramos este alimento…» (422 del backend).
 */
data class ManualMealUiState(
    val today: LocalDate,
    /** Día en que se creó la cuenta: el selector del día no ofrece días anteriores. */
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
    /** PT10 · el estado vacío de la búsqueda. */
    val notFound: Boolean get() = searchedTerm != null && !isSearching && results.isEmpty()

    enum class FoodError { NOT_CHOSEN, NOT_RESOLVED }
}

sealed interface ManualMealEvent {
    data class Logged(val outcome: MealLogOutcome) : ManualMealEvent

    /** Modo «elegir alimento» (PT8): vuelve con el alimento elegido. */
    data class FoodPicked(val referenceFoodId: Long, val name: String) : ManualMealEvent
}
