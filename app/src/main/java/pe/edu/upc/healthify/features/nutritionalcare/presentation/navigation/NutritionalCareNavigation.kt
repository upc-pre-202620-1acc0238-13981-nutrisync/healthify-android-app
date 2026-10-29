package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.MyPlanScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PlanVersionsScreen

/** PT4 · Mi plan (pantalla completa sobre el shell del paciente). */
@Serializable
data object MyPlanRoute

/**
 * PT4.1 · Versiones anteriores.
 *
 * @param version la versión cuyo detalle se abre al llegar (desde una consulta anterior de PT25); `null` muestra la lista.
 */
@Serializable
data class PlanVersionsRoute(val version: Int? = null) {
    companion object {
        const val ARG_VERSION = "version"
    }
}

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
