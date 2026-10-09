package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PatientRosterUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.RosterItem
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.PatientRosterViewModel
import java.time.Instant

/**
 * PR1 · Mi cartera de pacientes (pestaña «Pacientes» del shell del nutricionista). Tocar un paciente abre su ficha:
 * PAC-0 si aún no tiene datos base, PAC-1 si ya los tiene (lo decide el resumen, RM-2).
 */
@Composable
fun PatientRosterScreen(
    onInvitePatient: () -> Unit,
    onOpenPatient: (patientId: Long, careLinkId: Long, fullName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientRosterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PatientRosterContent(
        state = state,
        onInvitePatient = onInvitePatient,
        onOpenPatient = { onOpenPatient(it.patientId, it.careLinkId, it.fullName) },
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@Composable
fun PatientRosterContent(
    state: PatientRosterUiState,
    onInvitePatient: () -> Unit,
    onOpenPatient: (RosterItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.roster_title))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val patients = state.patients
            when {
                patients != null && patients.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space12),
                ) {
                    if (state.isOffline) {
                        item(key = "offline") { OfflineBanner(type = OfflineBannerType.RequiresNetwork) }
                    }
                    items(patients, key = { it.patientId }) { patient ->
                        RosterCard(patient = patient, onClick = { onOpenPatient(patient) })
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
                                title = stringResource(R.string.roster_offline_title),
                                text = stringResource(R.string.roster_offline_body),
                                actionLabel = stringResource(R.string.ds_retry),
                                onAction = onRetry,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
                        state.isEmpty -> {
                            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                            EmptyState(
                                title = stringResource(R.string.roster_empty_title),
                                text = stringResource(R.string.roster_empty_body),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        // «Invitar paciente» fijo abajo (sobre la NavigationBar); sin conexión no se puede generar el código.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(dimens.space16),
        ) {
            HealthifyButton(
                text = stringResource(R.string.roster_invite),
                onClick = onInvitePatient,
                enabled = !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val SKELETON_ROWS = 4

@Composable
private fun RosterCard(patient: RosterItem, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(text = patient.fullName, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                // DECISIÓN PR1: «Vinculada desde…» del frame en forma neutra (la app no sabe el género del paciente).
                Text(
                    text = stringResource(R.string.roster_linked_since, patient.linkedSince.mediumDateText()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            // DECISIÓN PR1: el chip «Nueva» de PAC-0 también marca en la cartera a quien aún no tiene datos base o plan.
            if (patient.isNew) {
                StatusChip(type = StatusChipType.PendingConfirmation, label = stringResource(R.string.patient_new_chip))
            }
            Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

private val previewPatients = listOf(
    RosterItem(1, 11, "Ana Flores", Instant.parse("2026-03-12T15:00:00Z"), isNew = false),
    RosterItem(2, 12, "Carlos Quispe", Instant.parse("2026-06-02T15:00:00Z"), isNew = false),
    RosterItem(3, 13, "Luz Ramírez", Instant.parse("2026-08-28T15:00:00Z"), isNew = true),
)

@Preview(name = "PR1 · Mi cartera", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterPreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, patients = previewPatients),
            onInvitePatient = {},
            onOpenPatient = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR1.L · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterLoadingPreview() {
    HealthifyTheme {
        PatientRosterContent(state = PatientRosterUiState(), onInvitePatient = {}, onOpenPatient = {}, onRetry = {})
    }
}

@Preview(name = "PR1.V · Sin pacientes", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterEmptyPreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, patients = emptyList()),
            onInvitePatient = {},
            onOpenPatient = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR1.O · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterOfflinePreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, isOffline = true, loadFailed = true),
            onInvitePatient = {},
            onOpenPatient = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR1 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterErrorPreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, loadFailed = true),
            onInvitePatient = {},
            onOpenPatient = {},
            onRetry = {},
        )
    }
}
