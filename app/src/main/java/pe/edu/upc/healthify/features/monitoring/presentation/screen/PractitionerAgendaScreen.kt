package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaDialog
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaRow
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.AgendaEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.PractitionerAgendaViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Insignia de fecha de cada consulta (frame PR17.0).
private val DateBadgeSize = 44.dp

data class AgendaActions(
    val onSchedule: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onVisitSelected: (AgendaRow) -> Unit = {},
    val onSheetDismissed: () -> Unit = {},
    val onReschedule: () -> Unit = {},
    val onCancelRequested: () -> Unit = {},
    val onCancelConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)

/**
 * PR17.0 · Agenda (pestaña «Agenda» del shell del nutricionista). «Agendar consulta» abre PR17; el Snackbar PR17.0-S lo
 * muestra el shell al volver.
 *
 * @param onCancelled el shell muestra «Consulta cancelada».
 */
@Composable
fun PractitionerAgendaScreen(
    onSchedule: () -> Unit,
    onReschedule: (AgendaRow) -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PractitionerAgendaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is AgendaEvent.OpenReschedule -> onReschedule(event.row)
            AgendaEvent.Cancelled -> onCancelled()
        }
    }
    PractitionerAgendaContent(
        state = state,
        actions = AgendaActions(
            onSchedule = onSchedule,
            onRetry = viewModel::onRetry,
            onVisitSelected = viewModel::onVisitSelected,
            onSheetDismissed = viewModel::onSheetDismissed,
            onReschedule = viewModel::onReschedule,
            onCancelRequested = viewModel::onCancelRequested,
            onCancelConfirmed = viewModel::onCancelConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun PractitionerAgendaContent(state: AgendaUiState, actions: AgendaActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.agenda_title))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val visits = state.visits
            when {
                visits != null && visits.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space12),
                ) {
                    if (state.isOffline) {
                        item(key = "offline") { OfflineBanner(type = OfflineBannerType.RequiresNetwork) }
                    }
                    item(key = "header") {
                        Text(
                            text = stringResource(R.string.agenda_upcoming),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    items(visits, key = { it.followUpId }) { row ->
                        AgendaCard(row = row, onClick = { actions.onVisitSelected(row) }, enabled = !state.isOffline)
                    }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space12),
                ) {
                    when {
                        state.needsConnection -> {
                            OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                            EmptyState(
                                title = stringResource(R.string.practitioner_offline_title),
                                text = stringResource(R.string.agenda_offline_body),
                                actionLabel = stringResource(R.string.ds_retry),
                                onAction = actions.onRetry,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
                        state.isEmpty -> {
                            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                            // El CTA del vacío es el mismo «Agendar consulta» fijo de abajo.
                            EmptyState(
                                title = stringResource(R.string.agenda_empty_title),
                                text = stringResource(R.string.agenda_empty_body),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.loadFailed -> ErrorState(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        // «Agendar consulta» fijo abajo (sobre la NavigationBar); sin conexión no se puede agendar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(dimens.space16),
        ) {
            HealthifyButton(
                text = stringResource(R.string.agenda_schedule),
                onClick = actions.onSchedule,
                enabled = !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    val selected = state.selected
    if (selected != null && state.dialog == null) {
        HealthifyBottomSheet(onDismissRequest = actions.onSheetDismissed) {
            VisitSheetContent(row = selected, onReschedule = actions.onReschedule, onCancel = actions.onCancelRequested)
        }
    }
    when (state.dialog) {
        AgendaDialog.CONFIRM_CANCEL -> HealthifyDialog(
            title = stringResource(R.string.agenda_cancel_dialog_title),
            text = stringResource(R.string.agenda_cancel_dialog_body),
            confirmLabel = stringResource(R.string.agenda_cancel_confirm),
            onConfirm = actions.onCancelConfirmed,
            onDismiss = actions.onDialogDismissed,
            destructive = true,
            confirmLoading = state.isCancelling,
        )
        AgendaDialog.NOT_CANCELLABLE -> HealthifyDialog(
            title = stringResource(R.string.agenda_not_cancellable_title),
            text = stringResource(R.string.agenda_not_cancellable_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onDialogDismissed,
            onDismiss = actions.onDialogDismissed,
            dismissLabel = null,
        )
        AgendaDialog.SERVER_ERROR -> ServerErrorDialog(onRetry = actions.onCancelConfirmed, onDismiss = actions.onDialogDismissed)
        null -> Unit
    }
}

@Composable
private fun AgendaCard(row: AgendaRow, onClick: () -> Unit, enabled: Boolean) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(onClick = if (enabled) onClick else null, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBadge(row.scheduledFor)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(
                    text = row.patientName ?: stringResource(R.string.inbox_unknown_patient),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.agenda_row_time, row.scheduledFor.weekdayText(), row.scheduledFor.shortTimeText()),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** «18 / SEPT»: día y mes abreviado (sin punto) en mayúsculas. */
@Composable
private fun DateBadge(moment: Instant) {
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val date = moment.atZone(ZoneId.systemDefault())
    Column(
        modifier = Modifier
            .size(DateBadgeSize)
            .clip(MaterialTheme.shapes.small)
            .background(scheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = DateTimeFormatter.ofPattern("dd", locale).format(date),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
        Text(
            text = DateTimeFormatter.ofPattern("MMM", locale).format(date).replace(".", "").uppercase(locale),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/** «Jueves», con mayúscula inicial. */
@Composable
@ReadOnlyComposable
private fun Instant.weekdayText(): String {
    val locale = currentLocale()
    return DateTimeFormatter.ofPattern("EEEE", locale).format(atZone(ZoneId.systemDefault()))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@Composable
private fun VisitSheetContent(row: AgendaRow, onReschedule: () -> Unit, onCancel: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) {
        Text(
            text = row.patientName ?: stringResource(R.string.inbox_unknown_patient),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.agenda_row_time, row.scheduledFor.weekdayText(), row.scheduledFor.shortTimeText()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HealthifyButton(
            text = stringResource(R.string.agenda_reschedule),
            onClick = onReschedule,
            style = HealthifyButtonStyle.Tonal,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.agenda_cancel),
            onClick = onCancel,
            style = HealthifyButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val SKELETON_ROWS = 3

private val previewVisits = listOf(
    AgendaRow(1, 11, "Ana Flores", Instant.parse("2026-09-18T15:00:00Z"), emptySet()),
    AgendaRow(2, 12, "Carlos Quispe", Instant.parse("2026-09-22T21:30:00Z"), emptySet()),
    AgendaRow(3, 13, "Luz Ramírez", Instant.parse("2026-10-01T14:00:00Z"), emptySet()),
)

@Preview(name = "PR17.0 · Agenda", widthDp = 360, heightDp = 800)
@Composable
private fun AgendaPreview() {
    HealthifyTheme {
        PractitionerAgendaContent(state = AgendaUiState(isLoading = false, visits = previewVisits), actions = AgendaActions())
    }
}

@Preview(name = "PR17.0 · Vacía", widthDp = 360, heightDp = 800)
@Composable
private fun AgendaEmptyPreview() {
    HealthifyTheme {
        PractitionerAgendaContent(state = AgendaUiState(isLoading = false, visits = emptyList()), actions = AgendaActions())
    }
}

@Preview(name = "PR17.0 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun AgendaLoadingPreview() {
    HealthifyTheme { PractitionerAgendaContent(state = AgendaUiState(), actions = AgendaActions()) }
}

@Preview(name = "PR17.0 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun AgendaOfflinePreview() {
    HealthifyTheme {
        PractitionerAgendaContent(
            state = AgendaUiState(isLoading = false, isOffline = true, loadFailed = true),
            actions = AgendaActions(),
        )
    }
}

@Preview(name = "PR17.0 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun AgendaErrorPreview() {
    HealthifyTheme {
        PractitionerAgendaContent(state = AgendaUiState(isLoading = false, loadFailed = true), actions = AgendaActions())
    }
}

@Preview(name = "PR17.0 · Consulta elegida", widthDp = 360, heightDp = 320)
@Composable
private fun AgendaSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame { VisitSheetContent(row = previewVisits.first(), onReschedule = {}, onCancel = {}) }
    }
}
