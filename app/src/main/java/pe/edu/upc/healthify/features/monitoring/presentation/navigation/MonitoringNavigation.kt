package pe.edu.upc.healthify.features.monitoring.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.monitoring.presentation.screen.CheckInScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.ConsistencyEscalationInfoScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.ConsistencyNoticeScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.FollowUpPreparationScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.HowAmITodayScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.MyConsultationsScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.WeeklySummaryScreen

/** How am I doing today. */
@Serializable
data object HowAmITodayRoute

/** Something doesn't add up (only from the home card, with an active alert). */
@Serializable
data object ConsistencyNoticeRoute

/** What happens if this keeps up. */
@Serializable
data object ConsistencyEscalationInfoRoute

/** Your visit - how to prepare (from the home "Next visit" row). */
@Serializable
data class FollowUpPreparationRoute(val followUpId: Long) {
    companion object {
        const val ARG_FOLLOW_UP_ID = "followUpId"
    }
}

/** My visits (from the profile's "My visits" and the preparation screen's "See my visits"). */
@Serializable
data object MyConsultationsRoute

/** Tell them how it went (from My visits "Reply" or the sent reply's "Edit my reply"). */
@Serializable
data class CheckInRoute(val followUpId: Long) {
    companion object {
        const val ARG_FOLLOW_UP_ID = "followUpId"
    }
}

/** Your week - AI summary (from the "Your week" card). */
@Serializable
data object WeeklySummaryRoute

data class MonitoringNavigationCallbacks(
    val onBack: () -> Unit,
    /** "Review my diary" -> the shell's Diary tab. */
    val onReviewDiary: () -> Unit,
    /** -> "What happens if this keeps up" (pushed on top). */
    val onOpenEscalationInfo: () -> Unit,
    /** "See my visits" -> My visits. */
    val onOpenMyConsultations: () -> Unit,
    /** My visits -> the check-in form. */
    val onOpenCheckIn: (followUpId: Long) -> Unit,
    /** Past visit -> the plan screen showing the version published in it. */
    val onOpenPlanVersion: (version: Int) -> Unit,
)

/** Patient destinations of MonitoringAdherence in the root graph (full screens over the shell). */
fun NavGraphBuilder.monitoringDestinations(callbacks: MonitoringNavigationCallbacks) {
    composable<HowAmITodayRoute> { HowAmITodayScreen(onBack = callbacks.onBack) }
    composable<ConsistencyNoticeRoute> {
        ConsistencyNoticeScreen(
            onBack = callbacks.onBack,
            onReviewDiary = callbacks.onReviewDiary,
            onWhatItMeans = callbacks.onOpenEscalationInfo,
        )
    }
    composable<ConsistencyEscalationInfoRoute> { ConsistencyEscalationInfoScreen(onBack = callbacks.onBack) }
    composable<WeeklySummaryRoute> { WeeklySummaryScreen(onBack = callbacks.onBack) }
    composable<FollowUpPreparationRoute> {
        FollowUpPreparationScreen(onBack = callbacks.onBack, onOpenMyConsultations = callbacks.onOpenMyConsultations)
    }
    composable<MyConsultationsRoute> {
        MyConsultationsScreen(
            onBack = callbacks.onBack,
            onOpenCheckIn = callbacks.onOpenCheckIn,
            onOpenPlanVersion = callbacks.onOpenPlanVersion,
        )
    }
    // On send, the check-in form (and its error state) returns to My visits, which re-reads and shows the reply.
    composable<CheckInRoute> { CheckInScreen(onBack = callbacks.onBack, onSubmitted = callbacks.onBack) }
}
