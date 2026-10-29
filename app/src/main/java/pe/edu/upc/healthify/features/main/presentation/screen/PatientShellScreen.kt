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
 * Patient shell navigation into the root graph. These destinations are full screens over the shell (no bottom
 * bar).
 */
data class PatientShellCallbacks(
    /** Sign out (full screen in the root graph); signing out clears the whole back stack. */
    val onSignOut: () -> Unit,
    /** "Settings" menu item → patient settings (full screen in the root graph, with "Back"). */
    val onOpenSettings: () -> Unit,
    val onOpenMyPlan: () -> Unit,
    val onOpenHowAmIToday: () -> Unit,
    val onOpenConsistencyNotice: () -> Unit,
    val onOpenFollowUp: (followUpId: Long) -> Unit,
    /** Bar camera button and "Log meal" (Diary) → meal photo (or manual logging without AI photo recognition). */
    val onRegisterMeal: () -> Unit,
    /** Diary "Confirm portion" → that entry's portion screen. */
    val onConfirmPortion: (diaryEntryId: Long, date: LocalDate) -> Unit,
    val onOpenMealIdeas: () -> Unit,
    val onOpenPendingSync: () -> Unit,
    val onAnalyzePendingPhoto: (photoId: String) -> Unit,
    val onRegisterManually: () -> Unit,
    /** Progress "Log self weigh-in" → weigh-in screen. */
    val onRegisterSelfWeighIn: () -> Unit,
    /** Progress "Your week" card → weekly summary. */
    val onOpenWeeklySummary: () -> Unit,
    /** Appointment card and the "My appointments" menu item → appointments. */
    val onOpenMyConsultations: () -> Unit,
    /** Home bell → Reminders. */
    val onOpenReminders: () -> Unit,
)

/**
 * Patient shell with an active care link: NavigationBar Home · Diary · [camera] · Progress · Record, one NavHost
 * per tab (keeps each tab's state when switching) and the main menu (☰ on the tabs) that opens
 * from the right.
 *
 * @param requestedTab tab that a root graph screen asked to open on return (the "Review my diary" action);
 *   it opens once and is reported with [onRequestedTabHandled].
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

    // Every menu destination is a root graph screen: the menu closes before opening it.
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
                // The menu only opens from the tabs, which are not menu items.
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

/** Tab switch: a single copy of each destination, with its state saved and restored. */
internal fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview(name = "Patient shell", widthDp = 360, heightDp = 800)
@Composable
private fun PatientShellContentPreview() {
    HealthifyTheme {
        PatientShellContent(state = PatientShellUiState(), selected = PatientTab.Home, actions = PatientShellActions()) {
            PlaceholderScreen(title = "Inicio")
        }
    }
}

@Preview(name = "Patient shell - menu open", widthDp = 360, heightDp = 800)
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
