package pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.AdjustProposalScreen
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.ReviewItemScreen

/** An inbox item (opens the AI proposal view if the signal carries an AI-proposed plan). */
@Serializable
data class ReviewItemRoute(val reviewItemId: Long) {
    companion object {
        const val ARG_REVIEW_ITEM_ID = "reviewItemId"
    }
}

/** Adjust the AI-proposed plan. */
@Serializable
data class AdjustProposalRoute(val reviewItemId: Long) {
    companion object {
        const val ARG_REVIEW_ITEM_ID = "reviewItemId"
    }
}

data class ReviewNavigationCallbacks(
    val onBack: () -> Unit,
    /** Resolved (with or without a plan): back to the inbox, which shows the "Signal resolved" snackbar. */
    val onResolved: (patientName: String?) -> Unit,
    /** The item is gone: back to the inbox (which is reloaded). */
    val onClose: () -> Unit,
    val onOpenAdjust: (reviewItemId: Long) -> Unit,
    /** "Adjust the plan now" → patient profile. */
    val onOpenPatient: (patientId: Long, careLinkId: Long, patientName: String) -> Unit,
)

/** Review item and AI proposal adjustment in the root graph (full screens over the nutritionist shell). */
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
