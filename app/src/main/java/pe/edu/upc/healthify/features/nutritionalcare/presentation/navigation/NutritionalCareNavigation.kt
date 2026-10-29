package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.MyPlanScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PlanVersionsScreen

/** My plan (full screen over the patient shell). */
@Serializable
data object MyPlanRoute

/**
 * Previous versions.
 *
 * @param version the version whose detail opens on arrival (from a past consultation); `null` shows the list.
 */
@Serializable
data class PlanVersionsRoute(val version: Int? = null) {
    companion object {
        const val ARG_VERSION = "version"
    }
}

data class NutritionalCareNavigationCallbacks(
    val onBack: () -> Unit,
    /** My plan → previous versions (pushed). */
    val onOpenPlanVersions: () -> Unit,
)

/** NutritionalCare patient destinations in the root graph. */
fun NavGraphBuilder.nutritionalCareDestinations(callbacks: NutritionalCareNavigationCallbacks) {
    composable<MyPlanRoute> {
        MyPlanScreen(onBack = callbacks.onBack, onOpenVersions = callbacks.onOpenPlanVersions)
    }
    composable<PlanVersionsRoute> { PlanVersionsScreen(onBack = callbacks.onBack) }
}
