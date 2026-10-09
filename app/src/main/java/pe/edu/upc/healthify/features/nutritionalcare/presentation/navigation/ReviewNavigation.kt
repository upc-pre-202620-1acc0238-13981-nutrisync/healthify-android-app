package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.AdjustProposalScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.ReviewItemScreen

/** PR14 / PR14.IA · un ítem de la bandeja (abre PR14.IA si la señal trae plan propuesto por IA). */
@Serializable
data class ReviewItemRoute(val reviewItemId: Long) {
    companion object {
        const val ARG_REVIEW_ITEM_ID = "reviewItemId"
    }
}

/** PR14.IA-A · Ajustar el plan propuesto por IA. */
@Serializable
data class AdjustProposalRoute(val reviewItemId: Long) {
    companion object {
        const val ARG_REVIEW_ITEM_ID = "reviewItemId"
    }
}

data class ReviewNavigationCallbacks(
    val onBack: () -> Unit,
    /** Resuelto (PR14, PR14.IA o PR14.IA-A): vuelve a la bandeja y muestra PR13.1 «Señal resuelta». */
    val onResolved: (patientName: String?) -> Unit,
    /** El ítem ya no está: vuelve a la bandeja (que se relee). */
    val onClose: () -> Unit,
    val onOpenAdjust: (reviewItemId: Long) -> Unit,
    /** PR14 «Ajustar el plan ahora» → ficha del paciente. */
    val onOpenPatient: (patientId: Long, careLinkId: Long, patientName: String) -> Unit,
)

/** PR14, PR14.IA y PR14.IA-A en el grafo raíz (pantallas completas sobre el shell del nutricionista). */
fun NavGraphBuilder.reviewDestinations(callbacks: ReviewNavigationCallbacks) {
    composable<ReviewItemRoute> {
        ReviewItemScreen(
            onBack = callbacks.onBack,
            onResolved = callbacks.onResolved,
            onClose = callbacks.onClose,
            onOpenAdjust = callbacks.onOpenAdjust,
            onOpenPatient = callbacks.onOpenPatient,
        )
    }
    composable<AdjustProposalRoute> {
        AdjustProposalScreen(
            onBack = callbacks.onBack,
            onAssigned = callbacks.onResolved,
            onClose = callbacks.onClose,
        )
    }
}
