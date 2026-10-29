package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyEndDrawer
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.PractitionerNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PractitionerTab
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.PatientRosterScreen
import pe.edu.upc.healthify.features.main.presentation.components.NotificationPermissionRequest
import pe.edu.upc.healthify.features.main.presentation.components.PlaceholderScreen
import pe.edu.upc.healthify.features.main.presentation.components.PractitionerMenuContent
import pe.edu.upc.healthify.features.main.presentation.components.PractitionerMenuItem
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerAgendaRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerInboxRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerPatientsRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.route
import pe.edu.upc.healthify.features.main.presentation.state.PractitionerShellUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PractitionerShellViewModel
import pe.edu.upc.healthify.features.monitoring.presentation.screen.PractitionerAgendaScreen
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaRow
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.ReviewInboxScreen
import java.time.Instant

/** What the practitioner shell opens in the root graph (full screens over it). */
data class PractitionerShellCallbacks(
    /** Patients "Invite patient" → invitation screen. */
    val onInvitePatient: () -> Unit,
    /** Patients → patient record (its first tab depends on whether there is baseline data). */
    val onOpenPatient: (patientId: Long, careLinkId: Long, fullName: String) -> Unit,
    /** Inbox → review item (manual or AI). */
    val onOpenReviewItem: (reviewItemId: Long) -> Unit = {},
    /** Calendar "Schedule appointment" → scheduling. */
    val onScheduleFollowUp: () -> Unit = {},
    /** Calendar "Reschedule" → scheduling with the appointment prefilled. */
    val onRescheduleFollowUp: (AgendaRow) -> Unit = {},
    /** "Food catalog" menu item → food catalog. */
    val onOpenFoodCatalog: () -> Unit = {},
    /** "Settings" menu item → practitioner settings (full screen in the root graph, with "Back"). */
    val onOpenSettings: () -> Unit = {},
    /** "Sign out" menu item → sign-out confirmation. */
    val onSignOut: () -> Unit = {},
)

/** Snackbar another screen requests on return to the shell (and the tab opened to show it). */
sealed interface PractitionerShellMessage {
    /** "Signal resolved; the patient no longer appears in your inbox." */
    data class SignalResolved(val patientName: String?) : PractitionerShellMessage

    /** "Appointment scheduled: Thu 18 Sep, 10:00" (or rescheduled). */
    data class FollowUpScheduled(val scheduledFor: Instant, val rescheduled: Boolean) : PractitionerShellMessage

    /** The discharge returned to Patients. */
    data class PatientDischarged(val patientName: String) : PractitionerShellMessage

    /** Value for the shell's `SavedStateHandle` (text only). */
    fun encode(): String = when (this) {
        is SignalResolved -> "$SIGNAL_RESOLVED${patientName.orEmpty()}"
        is FollowUpScheduled -> "${if (rescheduled) FOLLOW_UP_RESCHEDULED else FOLLOW_UP_SCHEDULED}${scheduledFor.epochSecond}"
        is PatientDischarged -> "$PATIENT_DISCHARGED$patientName"
    }

    companion object {
        private const val SIGNAL_RESOLVED = "SignalResolved:"
        private const val FOLLOW_UP_SCHEDULED = "FollowUpScheduled:"
        private const val FOLLOW_UP_RESCHEDULED = "FollowUpRescheduled:"
        private const val PATIENT_DISCHARGED = "PatientDischarged:"

        fun decode(value: String): PractitionerShellMessage? = when {
            value.startsWith(SIGNAL_RESOLVED) ->
                SignalResolved(value.removePrefix(SIGNAL_RESOLVED).takeIf(String::isNotBlank))
            value.startsWith(FOLLOW_UP_SCHEDULED) -> value.removePrefix(FOLLOW_UP_SCHEDULED).toLongOrNull()
                ?.let { FollowUpScheduled(Instant.ofEpochSecond(it), rescheduled = false) }
            value.startsWith(FOLLOW_UP_RESCHEDULED) -> value.removePrefix(FOLLOW_UP_RESCHEDULED).toLongOrNull()
                ?.let { FollowUpScheduled(Instant.ofEpochSecond(it), rescheduled = true) }
            value.startsWith(PATIENT_DISCHARGED) -> PatientDischarged(value.removePrefix(PATIENT_DISCHARGED))
            else -> null
        }
    }
}

/**
 * Practitioner shell: NavigationBar Patients · Inbox · Calendar with its own NavHost and the main menu (☰ on the
 * tabs) that opens from the right. The return snackbars (signal resolved, appointment scheduled, discharge) show
 * above the NavigationBar.
 */
@Composable
fun PractitionerShellScreen(
    callbacks: PractitionerShellCallbacks,
    message: PractitionerShellMessage?,
    onMessageHandled: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: PractitionerShellViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationPermissionRequest()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selected = PractitionerTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route()::class) } == true
    } ?: PractitionerTab.Patients
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messageText = message?.let { messageText(it) }
    LaunchedEffect(message) {
        val request = message ?: return@LaunchedEffect
        onMessageHandled()
        navController.navigateToTab(
            when (request) {
                is PractitionerShellMessage.SignalResolved -> PractitionerTab.Inbox
                is PractitionerShellMessage.FollowUpScheduled -> PractitionerTab.Agenda
                is PractitionerShellMessage.PatientDischarged -> PractitionerTab.Patients
            }.route(),
        )
        messageText?.let { snackbarHostState.showSnackbar(it) }
    }
    val cancelledText = stringResource(R.string.agenda_cancelled)

    // Every menu destination is a root graph screen: the menu closes before opening it.
    val fromMenu = { action: () -> Unit ->
        viewModel.onCloseMenu()
        action()
    }
    val onOpenMenu = viewModel::onOpenMenu
    PractitionerShellContent(
        state = state,
        selected = selected,
        actions = PractitionerShellActions(
            onSelect = { tab -> navController.navigateToTab(tab.route()) },
            onCloseMenu = viewModel::onCloseMenu,
            onMenuItem = { item ->
                fromMenu(
                    when (item) {
                        PractitionerMenuItem.FoodCatalog -> callbacks.onOpenFoodCatalog
                        PractitionerMenuItem.Settings -> callbacks.onOpenSettings
                    },
                )
            },
            onSignOut = { fromMenu(callbacks.onSignOut) },
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        NavHost(navController = navController, startDestination = PractitionerPatientsRoute) {
            composable<PractitionerPatientsRoute> {
                PatientRosterScreen(
                    onInvitePatient = callbacks.onInvitePatient,
                    onOpenPatient = callbacks.onOpenPatient,
                    onOpenMenu = onOpenMenu,
                )
            }
            composable<PractitionerInboxRoute> {
                ReviewInboxScreen(onOpenItem = callbacks.onOpenReviewItem, onOpenMenu = onOpenMenu)
            }
            composable<PractitionerAgendaRoute> {
                PractitionerAgendaScreen(
                    onSchedule = callbacks.onScheduleFollowUp,
                    onReschedule = callbacks.onRescheduleFollowUp,
                    onCancelled = { scope.launch { snackbarHostState.showSnackbar(cancelledText) } },
                    onOpenMenu = onOpenMenu,
                )
            }
        }
    }
}

/** Snackbar text for [message] in the app language. */
@Composable
private fun messageText(message: PractitionerShellMessage): String = when (message) {
    is PractitionerShellMessage.SignalResolved -> message.patientName
        ?.let { stringResource(R.string.shell_signal_resolved, it) }
        ?: stringResource(R.string.shell_signal_resolved_generic)
    is PractitionerShellMessage.FollowUpScheduled -> {
        // «jue 18 sept.» in lowercase inside the sentence.
        val locale = currentLocale()
        val day = message.scheduledFor.weekdayDayMonthText().replaceFirstChar { it.lowercase(locale) }
        stringResource(
            if (message.rescheduled) R.string.shell_follow_up_rescheduled else R.string.shell_follow_up_scheduled,
            day,
            message.scheduledFor.shortTimeText(),
        )
    }
    is PractitionerShellMessage.PatientDischarged -> stringResource(R.string.shell_patient_discharged, message.patientName)
}

data class PractitionerShellActions(
    val onSelect: (PractitionerTab) -> Unit = {},
    val onCloseMenu: () -> Unit = {},
    val onMenuItem: (PractitionerMenuItem) -> Unit = {},
    val onSignOut: () -> Unit = {},
)

@Composable
fun PractitionerShellContent(
    state: PractitionerShellUiState,
    selected: PractitionerTab,
    actions: PractitionerShellActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable () -> Unit,
) {
    HealthifyEndDrawer(
        open = state.isMenuOpen,
        onDismissRequest = actions.onCloseMenu,
        drawerContent = {
            PractitionerMenuContent(
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
                // Above the tab's pinned action (88 dp in the Figma: 56 dp button + 16 dp above and below).
                HealthifySnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = HealthifyTheme.dimens.space64 + HealthifyTheme.dimens.space24),
                )
            }
            PractitionerNavigationBar(selected = selected, onSelect = actions.onSelect)
        }
    }
}

@Preview(name = "Practitioner shell", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerShellContentPreview() {
    HealthifyTheme {
        PractitionerShellContent(
            state = PractitionerShellUiState(),
            selected = PractitionerTab.Patients,
            actions = PractitionerShellActions(),
        ) {
            PlaceholderScreen(title = "Pacientes")
        }
    }
}

@Preview(name = "Practitioner main menu", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerShellMenuPreview() {
    HealthifyTheme {
        PractitionerShellContent(
            state = PractitionerShellUiState(fullName = "Lucía Paredes", avatarInitial = "L", isMenuOpen = true),
            selected = PractitionerTab.Patients,
            actions = PractitionerShellActions(),
        ) {
            PlaceholderScreen(title = "Pacientes")
        }
    }
}
