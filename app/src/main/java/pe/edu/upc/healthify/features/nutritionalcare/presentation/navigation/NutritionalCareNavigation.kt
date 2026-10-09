package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.MyPlanScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PlanVersionsScreen

/** PT4 · Mi plan (pantalla completa sobre el shell del paciente). */
@Serializable
data object MyPlanRoute

/** PT4.1 · Versiones anteriores. */
@Serializable
data object PlanVersionsRoute

data class NutritionalCareNavigationCallbacks(
    val onBack: () -> Unit,
    /** PT4 → PT4.1 (se apila). */
    val onOpenPlanVersions: () -> Unit,
)

/** Destinos del paciente de NutritionalCare en el grafo raíz. */
fun NavGraphBuilder.nutritionalCareDestinations(callbacks: NutritionalCareNavigationCallbacks) {
    composable<MyPlanRoute> {
        MyPlanScreen(onBack = callbacks.onBack, onOpenVersions = callbacks.onOpenPlanVersions)
    }
    composable<PlanVersionsRoute> { PlanVersionsScreen(onBack = callbacks.onBack) }
}
