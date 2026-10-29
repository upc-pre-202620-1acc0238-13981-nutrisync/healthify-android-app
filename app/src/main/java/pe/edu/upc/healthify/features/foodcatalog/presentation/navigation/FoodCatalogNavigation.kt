package pe.edu.upc.healthify.features.foodcatalog.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.foodcatalog.presentation.screen.AddLocalFoodScreen
import pe.edu.upc.healthify.features.foodcatalog.presentation.screen.FoodCatalogScreen

/** Food catalog (from Settings). */
@Serializable
data object FoodCatalogRoute

/** Add local food. */
@Serializable
data object AddLocalFoodRoute

/** Food catalog `SavedStateHandle` key: name of the food the add screen just created (for the Snackbar). */
const val KEY_ADDED_LOCAL_FOOD = "added_local_food"

data class FoodCatalogNavigationCallbacks(
    val onBack: () -> Unit,
    val onAddLocalFood: () -> Unit,
    /** Add local food saved: returns to the catalog leaving [name] in its `SavedStateHandle` ([KEY_ADDED_LOCAL_FOOD]). */
    val onLocalFoodAdded: (name: String) -> Unit,
)

/** Food catalog and add local food in the root graph (full screens over the nutritionist shell). */
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
