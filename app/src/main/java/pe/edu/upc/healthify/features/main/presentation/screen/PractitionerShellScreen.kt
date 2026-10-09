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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.PractitionerNavigationBar
import pe.edu.upc.healthify.core.designsystem.component.PractitionerTab
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.PatientRosterScreen
import pe.edu.upc.healthify.features.main.presentation.components.PlaceholderScreen
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerAgendaRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerInboxRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerPatientsRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.PractitionerSettingsRoute
import pe.edu.upc.healthify.features.main.presentation.navigation.route
import pe.edu.upc.healthify.features.monitoring.presentation.screen.PractitionerAgendaScreen
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaRow
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.ReviewInboxScreen
import java.time.Instant

/** Lo que el shell del nutricionista abre en el grafo raíz (pantallas completas sobre él). */
data class PractitionerShellCallbacks(
    /** PR1 «Invitar paciente» → PR2. */
    val onInvitePatient: () -> Unit,
    /** PR1 → ficha del paciente (PAC-0 o PAC-1 según tenga datos base). */
    val onOpenPatient: (patientId: Long, careLinkId: Long, fullName: String) -> Unit,
    /** PR13 → PR14 / PR14.IA. */
    val onOpenReviewItem: (reviewItemId: Long) -> Unit = {},
    /** PR17.0 «Agendar consulta» → PR17. */
    val onScheduleFollowUp: () -> Unit = {},
    /** PR17.0 «Reprogramar» → PR17 con la consulta prellenada. */
    val onRescheduleFollowUp: (AgendaRow) -> Unit = {},
    /** PR20 → PR15. */
    val onOpenFoodCatalog: () -> Unit = {},
    /** PR20 → PR20.1. */
    val onSignOut: () -> Unit = {},
)

/** Snackbar que otra pantalla pide al volver al shell (y la pestaña que se abre para mostrarlo). */
sealed interface PractitionerShellMessage {
    /** PR13.1 · «Señal resuelta. Ana Flores ya no aparece en tu bandeja.» */
    data class SignalResolved(val patientName: String?) : PractitionerShellMessage

    /** PR17.0-S · «Consulta agendada: jue 18 sept., 10:00» (o reprogramada). */
    data class FollowUpScheduled(val scheduledFor: Instant, val rescheduled: Boolean) : PractitionerShellMessage

    /** PR18 · el alta volvió a PR1. */
    data class PatientDischarged(val patientName: String) : PractitionerShellMessage

    /** Valor para el `SavedStateHandle` del shell (solo texto). */
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
 * Shell del nutricionista: NavigationBar Pacientes (PR1) · Bandeja (PR13) · Agenda (PR17.0) · Ajustes (PR20) con su
 * propio NavHost. Los Snackbars de vuelta (PR13.1, PR17.0-S, alta) se muestran sobre la NavigationBar.
 */
@Composable
fun PractitionerShellScreen(
    callbacks: PractitionerShellCallbacks,
    message: PractitionerShellMessage?,
    onMessageHandled: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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

    PractitionerShellContent(
        selected = selected,
        onSelect = { tab -> navController.navigateToTab(tab.route()) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        NavHost(navController = navController, startDestination = PractitionerPatientsRoute) {
            composable<PractitionerPatientsRoute> {
                PatientRosterScreen(
                    onInvitePatient = callbacks.onInvitePatient,
                    onOpenPatient = callbacks.onOpenPatient,
                )
            }
            composable<PractitionerInboxRoute> {
                ReviewInboxScreen(onOpenItem = callbacks.onOpenReviewItem)
            }
            composable<PractitionerAgendaRoute> {
                PractitionerAgendaScreen(
                    onSchedule = callbacks.onScheduleFollowUp,
                    onReschedule = callbacks.onRescheduleFollowUp,
                    onCancelled = { scope.launch { snackbarHostState.showSnackbar(cancelledText) } },
                )
            }
            composable<PractitionerSettingsRoute> {
                PractitionerSettingsScreen(onOpenFoodCatalog = callbacks.onOpenFoodCatalog, onSignOut = callbacks.onSignOut)
            }
        }
    }
}

/** Texto del Snackbar de [message] en el idioma de la app. */
@Composable
private fun messageText(message: PractitionerShellMessage): String = when (message) {
    is PractitionerShellMessage.SignalResolved -> message.patientName
        ?.let { stringResource(R.string.shell_signal_resolved, it) }
        ?: stringResource(R.string.shell_signal_resolved_generic)
    is PractitionerShellMessage.FollowUpScheduled -> {
        // «jue 18 sept.» en minúscula dentro de la frase.
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

@Composable
fun PractitionerShellContent(
    selected: PractitionerTab,
    onSelect: (PractitionerTab) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
            // Sobre la acción fija de la pestaña (88 dp del Figma: botón de 56 dp + 16 dp arriba y abajo).
            HealthifySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = HealthifyTheme.dimens.space64 + HealthifyTheme.dimens.space24),
            )
        }
        PractitionerNavigationBar(selected = selected, onSelect = onSelect)
    }
}

@Preview(name = "Shell nutricionista", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerShellContentPreview() {
    HealthifyTheme {
        PractitionerShellContent(selected = PractitionerTab.Patients, onSelect = {}) {
            PlaceholderScreen(title = "Pacientes")
        }
    }
}
