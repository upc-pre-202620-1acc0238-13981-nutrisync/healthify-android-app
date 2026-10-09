package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pe.edu.upc.healthify.core.designsystem.component.PatientNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.presentation.screen.DiaryCallbacks
import pe.edu.upc.healthify.features.intake.presentation.screen.DiaryScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.WeightTrendScreen
import pe.edu.upc.healthify.features.main.presentation.components.PlaceholderScreen
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientDiaryRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientHomeRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientProgressRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientRecordRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientSettingsRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.route
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PatientRecordScreen
import java.time.LocalDate

/**
 * Navegación del shell del paciente hacia el grafo raíz. PT4, PT12, PT13.2, PT15, PT16, PT21.1, PT21.IA, PT22 y
 * PT25–PT25.2 son pantallas completas sobre el shell (sin barra inferior).
 */
data class PatientShellCallbacks(
    /** PT21.1 (pantalla completa del grafo raíz); al cerrar sesión limpia todo el back stack. */
    val onSignOut: () -> Unit,
    /** PT21.V (diálogo del grafo raíz sobre el shell). */
    val onSwitchPractitioner: () -> Unit,
    /** PT23 (pantalla completa del grafo raíz sobre el shell). */
    val onWithdrawConsent: () -> Unit,
    val onOpenMyPlan: () -> Unit,
    val onOpenHowAmIToday: () -> Unit,
    val onOpenConsistencyNotice: () -> Unit,
    val onOpenFollowUp: (followUpId: Long) -> Unit,
    /** «Registrar comida» (Inicio y Diario) → PT5 (o PT9 si la foto con IA no está disponible). */
    val onRegisterMeal: () -> Unit,
    /** PT14 «Confirmar porción» → PT8 de esa entrada. */
    val onConfirmPortion: (diaryEntryId: Long, date: LocalDate) -> Unit,
    val onOpenMealIdeas: () -> Unit,
    val onOpenPendingSync: () -> Unit,
    val onAnalyzePendingPhoto: (photoId: String) -> Unit,
    val onRegisterManually: () -> Unit,
    /** PT13 «Registrar autopesaje» → PT12. */
    val onRegisterSelfWeighIn: () -> Unit,
    /** PT13 card «Tu semana» → PT13.2. */
    val onOpenWeeklySummary: () -> Unit,
    /** PT20 «Mis consultas» → PT25. */
    val onOpenMyConsultations: () -> Unit,
    /** PT21 «Recordatorios» → PT22. */
    val onOpenReminders: () -> Unit,
    /** PT21 «Funciones con IA» → PT21.IA. */
    val onOpenAiFeatures: () -> Unit,
)

/**
 * Shell del paciente con vínculo activo: NavigationBar Inicio · Diario · Progreso · Expediente · Ajustes y un
 * NavHost propio por pestaña (conserva el estado de cada una al cambiar).
 *
 * @param requestedTab pestaña que pidió abrir una pantalla del grafo raíz al volver (PT16 «Revisar mi diario»);
 *   se abre una vez y se avisa con [onRequestedTabHandled].
 */
@Composable
fun PatientShellScreen(
    callbacks: PatientShellCallbacks,
    modifier: Modifier = Modifier,
    requestedTab: PatientTab? = null,
    onRequestedTabHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            navController.navigateToTab(requestedTab.route())
            onRequestedTabHandled()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selected = PatientTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route()::class) } == true
    } ?: PatientTab.Home

    PatientShellContent(
        selected = selected,
        onSelect = { tab -> navController.navigateToTab(tab.route()) },
        modifier = modifier,
    ) {
        NavHost(navController = navController, startDestination = PatientHomeRoute) {
            composable<PatientHomeRoute> {
                PatientHomeScreen(
                    onOpenMyPlan = callbacks.onOpenMyPlan,
                    onRegisterMeal = callbacks.onRegisterMeal,
                    onOpenHowAmIToday = callbacks.onOpenHowAmIToday,
                    onOpenFollowUp = callbacks.onOpenFollowUp,
                    onOpenConsistencyNotice = callbacks.onOpenConsistencyNotice,
                )
            }
            composable<PatientDiaryRoute> {
                DiaryScreen(
                    callbacks = DiaryCallbacks(
                        onRegisterMeal = callbacks.onRegisterMeal,
                        onConfirmPortion = { entry -> callbacks.onConfirmPortion(entry.id.value, entry.localTimestamp.localDate) },
                        onOpenIdeas = callbacks.onOpenMealIdeas,
                        onOpenPendingSync = callbacks.onOpenPendingSync,
                        onAnalyzePendingPhoto = callbacks.onAnalyzePendingPhoto,
                        onRegisterManually = callbacks.onRegisterManually,
                    ),
                )
            }
            composable<PatientProgressRoute> {
                WeightTrendScreen(
                    onRegisterWeighIn = callbacks.onRegisterSelfWeighIn,
                    onOpenWeeklySummary = callbacks.onOpenWeeklySummary,
                )
            }
            composable<PatientRecordRoute> { PatientRecordScreen(onOpenMyConsultations = callbacks.onOpenMyConsultations) }
            composable<PatientSettingsRoute> {
                PatientSettingsScreen(
                    callbacks = PatientSettingsCallbacks(
                        onOpenReminders = callbacks.onOpenReminders,
                        onOpenAiFeatures = callbacks.onOpenAiFeatures,
                        onOpenPendingSync = callbacks.onOpenPendingSync,
                        onWithdrawConsent = callbacks.onWithdrawConsent,
                        onSwitchPractitioner = callbacks.onSwitchPractitioner,
                        onSignOut = callbacks.onSignOut,
                    ),
                )
            }
        }
    }
}

@Composable
fun PatientShellContent(
    selected: PatientTab,
    onSelect: (PatientTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            content()
        }
        PatientNavigationBar(selected = selected, onSelect = onSelect)
    }
}

/** Cambio de pestaña: una sola copia de cada destino y su estado guardado/restaurado. */
internal fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview(name = "Shell paciente", widthDp = 360, heightDp = 800)
@Composable
private fun PatientShellContentPreview() {
    HealthifyTheme {
        PatientShellContent(selected = PatientTab.Home, onSelect = {}) {
            PlaceholderScreen(title = "Inicio")
        }
    }
}
