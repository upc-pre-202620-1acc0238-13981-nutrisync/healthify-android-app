package pe.edu.upc.healthify.features.foodcatalog.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.foodcatalog.presentation.screen.AddLocalFoodScreen
import pe.edu.upc.healthify.features.foodcatalog.presentation.screen.FoodCatalogScreen

/** PR15 · Catálogo de alimentos (desde PR20 Ajustes). */
@Serializable
data object FoodCatalogRoute

/** PR15.1 · Agregar alimento local. */
@Serializable
data object AddLocalFoodRoute

/** Clave del `SavedStateHandle` de PR15: nombre del alimento que PR15.1 acaba de crear (para el Snackbar). */
const val KEY_ADDED_LOCAL_FOOD = "added_local_food"

data class FoodCatalogNavigationCallbacks(
    val onBack: () -> Unit,
    val onAddLocalFood: () -> Unit,
    /** PR15.1 guardado: vuelve a PR15 dejando [name] en su `SavedStateHandle` ([KEY_ADDED_LOCAL_FOOD]). */
    val onLocalFoodAdded: (name: String) -> Unit,
)

/** PR15 y PR15.1 en el grafo raíz (pantallas completas sobre el shell del nutricionista). */
fun NavGraphBuilder.foodCatalogDestinations(callbacks: FoodCatalogNavigationCallbacks) {
    composable<FoodCatalogRoute> { entry ->
        val addedName by entry.savedStateHandle.getStateFlow<String?>(KEY_ADDED_LOCAL_FOOD, null).collectAsStateWithLifecycle()
        FoodCatalogScreen(
            onBack = callbacks.onBack,
            onAddLocalFood = callbacks.onAddLocalFood,
            addedFoodName = addedName,
            onAddedFoodHandled = { entry.savedStateHandle[KEY_ADDED_LOCAL_FOOD] = null },
        )
    }
    composable<AddLocalFoodRoute> {
        AddLocalFoodScreen(onBack = callbacks.onBack, onAdded = callbacks.onLocalFoodAdded)
    }
}
