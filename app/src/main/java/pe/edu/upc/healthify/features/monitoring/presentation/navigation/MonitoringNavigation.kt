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

/** PT15 · Cómo voy hoy. */
@Serializable
data object HowAmITodayRoute

/** PT16 · Algo no cuadra (solo desde la tarjeta de PT3, con alerta activa). */
@Serializable
data object ConsistencyNoticeRoute

/** PT17 · Qué pasa si esto sigue así. */
@Serializable
data object ConsistencyEscalationInfoRoute

/** PT25.1 · Tu consulta — cómo prepararte (desde «Próxima consulta» de PT3). */
@Serializable
data class FollowUpPreparationRoute(val followUpId: Long) {
    companion object {
        const val ARG_FOLLOW_UP_ID = "followUpId"
    }
}

/** PT25 · Mis consultas (desde PT20 «Mis consultas» y PT25.1 «Ver mis consultas»). */
@Serializable
data object MyConsultationsRoute

/** PT25.2 · Cuéntale cómo te fue (desde PT25 «Responder» o PT25.3 «Editar mi respuesta»). */
@Serializable
data class CheckInRoute(val followUpId: Long) {
    companion object {
        const val ARG_FOLLOW_UP_ID = "followUpId"
    }
}

/** PT13.2 · Tu semana — resumen con IA (desde la card de PT13). */
@Serializable
data object WeeklySummaryRoute

data class MonitoringNavigationCallbacks(
    val onBack: () -> Unit,
    /** PT16 «Revisar mi diario» → pestaña Diario del shell (PT14). */
    val onReviewDiary: () -> Unit,
    /** PT16 → PT17 (se apila). */
    val onOpenEscalationInfo: () -> Unit,
    /** PT25.1 «Ver mis consultas» → PT25. */
    val onOpenMyConsultations: () -> Unit,
    /** PT25 → PT25.2. */
    val onOpenCheckIn: (followUpId: Long) -> Unit,
    /** PT25 consulta anterior → PT4.1. */
    val onOpenPlanVersions: () -> Unit,
)

/** Destinos del paciente de MonitoringAdherence en el grafo raíz (pantallas completas sobre el shell). */
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
            onOpenPlanVersions = callbacks.onOpenPlanVersions,
        )
    }
    // PT25.2 (y PT25.2.E dentro de la misma pantalla): al enviar vuelve a PT25, que relee y muestra PT25.3.
    composable<CheckInRoute> { CheckInScreen(onBack = callbacks.onBack, onSubmitted = callbacks.onBack) }
}
