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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pe.edu.upc.healthify.core.designsystem.component.HealthifyEndDrawer
import pe.edu.upc.healthify.core.designsystem.component.PatientNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.presentation.screen.DiaryCallbacks
import pe.edu.upc.healthify.features.intake.presentation.screen.DiaryScreen
import pe.edu.upc.healthify.features.intake.presentation.screen.WeightTrendScreen
import pe.edu.upc.healthify.features.main.presentation.components.NotificationPermissionRequest
import pe.edu.upc.healthify.features.main.presentation.components.PatientMenuContent
import pe.edu.upc.healthify.features.main.presentation.components.PatientMenuItem
import pe.edu.upc.healthify.features.main.presentation.components.PlaceholderScreen
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientDiaryRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientHomeRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientProgressRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PatientRecordRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.route
import pe.edu.upc.healthify.features.main.presentation.state.PatientShellUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PatientShellViewModel
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PatientRecordScreen
import java.time.LocalDate

/**
 * Navegación del shell del paciente hacia el grafo raíz. PT4, PT12, PT13.2, PT15, PT16, PT21.1, PT21.IA, PT22 y
 * PT25–PT25.2 son pantallas completas sobre el shell (sin barra inferior).
 */
data class PatientShellCallbacks(
    /** PT21.1 (pantalla completa del grafo raíz); al cerrar sesión limpia todo el back stack. */
    val onSignOut: () -> Unit,
    /** Menú «Ajustes» → PT21 (pantalla completa del grafo raíz, con «Atrás»). */
    val onOpenSettings: () -> Unit,
    val onOpenMyPlan: () -> Unit,
    val onOpenHowAmIToday: () -> Unit,
    val onOpenConsistencyNotice: () -> Unit,
    val onOpenFollowUp: (followUpId: Long) -> Unit,
    /** Botón cámara de la barra y «Registrar comida» (Diario) → PT5 (o PT9 si la foto con IA no está disponible). */
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
    /** PT20 y menú «Mis consultas» → PT25. */
    val onOpenMyConsultations: () -> Unit,
    /** Campana de Inicio → PT22 Recordatorios. */
    val onOpenReminders: () -> Unit,
)

/**
 * Shell del paciente con vínculo activo: NavigationBar Inicio · Diario · [cámara] · Progreso · Expediente, un NavHost
 * propio por pestaña (conserva el estado de cada una al cambiar) y el menú principal (☰ de las pestañas) que abre
 * desde la derecha.
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
    viewModel: PatientShellViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationPermissionRequest()
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

    // Cada destino del menú es una pantalla del grafo raíz: el menú se cierra antes de abrirla.
    val fromMenu = { action: () -> Unit ->
        viewModel.onCloseMenu()
        action()
    }
    val onOpenMenu = viewModel::onOpenMenu
    PatientShellContent(
        state = state,
        selected = selected,
        actions = PatientShellActions(
            onSelect = { tab -> navController.navigateToTab(tab.route()) },
            onRegisterMeal = callbacks.onRegisterMeal,
            onCloseMenu = viewModel::onCloseMenu,
            onMenuItem = { item ->
                fromMenu(
                    when (item) {
                        PatientMenuItem.MyPlan -> callbacks.onOpenMyPlan
                        PatientMenuItem.MyConsultations -> callbacks.onOpenMyConsultations
                        PatientMenuItem.Pending -> callbacks.onOpenPendingSync
                        PatientMenuItem.Settings -> callbacks.onOpenSettings
                    },
                )
            },
            onSignOut = { fromMenu(callbacks.onSignOut) },
        ),
        modifier = modifier,
    ) {
        NavHost(navController = navController, startDestination = PatientHomeRoute) {
            composable<PatientHomeRoute> {
                PatientHomeScreen(
                    onOpenMenu = onOpenMenu,
                    onOpenReminders = callbacks.onOpenReminders,
                    onOpenMyPlan = callbacks.onOpenMyPlan,
                    onOpenDiary = { navController.navigateToTab(PatientTab.Diary.route()) },
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
                        onOpenMenu = onOpenMenu,
                    ),
                )
            }
            composable<PatientProgressRoute> {
                WeightTrendScreen(
                    onOpenMenu = onOpenMenu,
                    onRegisterWeighIn = callbacks.onRegisterSelfWeighIn,
                    onOpenWeeklySummary = callbacks.onOpenWeeklySummary,
                )
            }
            composable<PatientRecordRoute> {
                PatientRecordScreen(onOpenMenu = onOpenMenu, onOpenMyConsultations = callbacks.onOpenMyConsultations)
            }
        }
    }
}

data class PatientShellActions(
    val onSelect: (PatientTab) -> Unit = {},
    val onRegisterMeal: () -> Unit = {},
    val onCloseMenu: () -> Unit = {},
    val onMenuItem: (PatientMenuItem) -> Unit = {},
    val onSignOut: () -> Unit = {},
)

@Composable
fun PatientShellContent(
    state: PatientShellUiState,
    selected: PatientTab,
    actions: PatientShellActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    HealthifyEndDrawer(
        open = state.isMenuOpen,
        onDismissRequest = actions.onCloseMenu,
        drawerContent = {
            PatientMenuContent(
                fullName = state.fullName,
                avatarInitial = state.avatarInitial,
                // El menú solo se abre desde las pestañas, que no son ítems del menú.
                selected = null,
                onSelect = actions.onMenuItem,
                onSignOut = actions.onSignOut,
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
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
            PatientNavigationBar(selected = selected, onSelect = actions.onSelect, onRegisterMeal = actions.onRegisterMeal)
        }
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
        PatientShellContent(state = PatientShellUiState(), selected = PatientTab.Home, actions = PatientShellActions()) {
            PlaceholderScreen(title = "Inicio")
        }
    }
}

@Preview(name = "Shell paciente · menú abierto", widthDp = 360, heightDp = 800)
@Composable
private fun PatientShellMenuPreview() {
    HealthifyTheme {
        PatientShellContent(
            state = PatientShellUiState(fullName = "María Flores", avatarInitial = "M", isMenuOpen = true),
            selected = PatientTab.Home,
            actions = PatientShellActions(),
        ) {
            PlaceholderScreen(title = "Inicio")
        }
    }
}
