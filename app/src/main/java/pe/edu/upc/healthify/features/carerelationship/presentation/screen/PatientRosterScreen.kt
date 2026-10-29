package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.RosterAttention
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PatientRosterUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.RosterItem
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.PatientRosterViewModel
import java.time.Instant

// Medidas del frame «N10 · Inicio — agrupada por acción».
private val AvatarSize = 40.dp
private val StackedAvatarSize = 32.dp
private val StackedAvatarOverlap = 10.dp
private const val STACKED_AVATARS = 3
private const val CHEVRON_UP = -90f

/** Acciones de PR1 (navegación + eventos del ViewModel). */
data class PatientRosterActions(
    val onInvitePatient: () -> Unit = {},
    val onOpenPatient: (RosterItem) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenMenu: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onToggleUpToDate: () -> Unit = {},
)

/**
 * PR1 · Mis pacientes (pestaña «Pacientes» del shell del nutricionista), agrupada por acción: quienes requieren algo
 * (con el motivo) y quienes están al día, plegados en una fila. Tocar un paciente abre su ficha: PAC-0 si aún no tiene
 * datos base, PAC-1 si ya los tiene y PAC-1.C con una consulta en curso (lo decide el resumen, RM-2).
 *
 * DECISIÓN PR1 (sin scroll): la pantalla no se desplaza; si una lista no cabe, se desplaza dentro de su tarjeta.
 *
 * @param onOpenMenu ☰ · menú principal del shell (PR1.D).
 */
@Composable
fun PatientRosterScreen(
    onInvitePatient: () -> Unit,
    onOpenPatient: (patientId: Long, careLinkId: Long, fullName: String) -> Unit,
    onOpenMenu: () -> Unit,
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
        actions = PatientRosterActions(
            onInvitePatient = onInvitePatient,
            onOpenPatient = { onOpenPatient(it.patientId, it.careLinkId, it.fullName) },
            onRetry = viewModel::onRetry,
            onOpenMenu = onOpenMenu,
            onQueryChange = viewModel::onQueryChange,
            onToggleUpToDate = viewModel::onToggleUpToDate,
        ),
        modifier = modifier,
    )
}

@Composable
fun PatientRosterContent(
    state: PatientRosterUiState,
    actions: PatientRosterActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(
            title = stringResource(R.string.roster_title),
            actions = {
                HealthifyIconButton(
                    icon = HealthifyIcons.Menu,
                    contentDescription = stringResource(R.string.menu_cd_open),
                    onClick = actions.onOpenMenu,
                )
            },
        )
        val patients = state.patients
        if (!patients.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                RosterSearchField(query = state.query, onQueryChange = actions.onQueryChange)
                RosterGroups(state = state, actions = actions)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            onAction = actions.onRetry,
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
                    state.loadFailed -> ErrorState(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth())
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
                onClick = actions.onInvitePatient,
                enabled = !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val SKELETON_ROWS = 4

/** El mismo campo que PR15 (catálogo): filtra la cartera por nombre en el teléfono. */
@Composable
private fun RosterSearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    HealthifyTextField(
        value = query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.roster_search),
        placeholder = stringResource(R.string.roster_search_placeholder),
        imeAction = ImeAction.Search,
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        trailingIcon = {
            Icon(HealthifyIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * «Requieren acción · N» (oculta si no hay nadie) y «Al día · N». Cada grupo ocupa lo que necesita; si no cabe, se
 * reparte el alto y su lista se desplaza dentro de la tarjeta.
 */
@Composable
private fun ColumnScope.RosterGroups(state: PatientRosterUiState, actions: PatientRosterActions) {
    if (state.noSearchResults) {
        EmptyState(
            title = stringResource(R.string.roster_no_results_title),
            text = stringResource(R.string.roster_no_results_body, state.query.trim()),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val needsAction = state.needsAction
    val upToDate = state.upToDate
    if (needsAction.isNotEmpty()) {
        HealthifyCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            GroupTitle(stringResource(R.string.roster_needs_action, needsAction.size))
            LazyColumn(modifier = Modifier.padding(horizontal = HealthifyTheme.dimens.space16)) {
                itemsIndexed(needsAction, key = { _, it -> it.patientId }) { index, patient ->
                    if (index > 0) CardDivider()
                    PatientRow(patient = patient, onClick = { actions.onOpenPatient(patient) })
                }
            }
        }
    }
    if (upToDate.isNotEmpty()) {
        val expanded = state.isUpToDateExpanded
        HealthifyCard(modifier = Modifier.fillMaxWidth().then(if (expanded) Modifier.weight(1f, fill = false) else Modifier)) {
            UpToDateRow(patients = upToDate, expanded = expanded, onClick = actions.onToggleUpToDate)
            if (expanded) {
                LazyColumn(modifier = Modifier.padding(horizontal = HealthifyTheme.dimens.space16)) {
                    itemsIndexed(upToDate, key = { _, it -> it.patientId }) { index, patient ->
                        if (index > 0) CardDivider()
                        PatientRow(patient = patient, onClick = { actions.onOpenPatient(patient) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    val dimens = HealthifyTheme.dimens
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space4)
            .semantics { heading() },
    )
}

/** «Ana Flores · Señal por revisar» (o, al día, solo el nombre). */
@Composable
private fun PatientRow(patient: RosterItem, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = dimens.space12),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = patient.fullName, color = patient.attention.avatarColor(), size = AvatarSize)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = patient.fullName, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            patient.attention?.let {
                Text(text = stringResource(it.labelRes()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

/** «Al día · 9 · Sin pendientes» con hasta tres avatares superpuestos; al tocarla se despliega en su sitio. */
@Composable
private fun UpToDateRow(patients: List<RosterItem>, expanded: Boolean, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val stacked = patients.take(STACKED_AVATARS)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(dimens.space16),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(StackedAvatarSize + (StackedAvatarSize - StackedAvatarOverlap) * (stacked.size - 1))
                .clearAndSetSemantics {},
        ) {
            stacked.forEachIndexed { index, patient ->
                Avatar(
                    name = patient.fullName,
                    color = scheme.surfaceContainer,
                    size = StackedAvatarSize,
                    modifier = Modifier.offset(x = (StackedAvatarSize - StackedAvatarOverlap) * index),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(
                text = stringResource(R.string.roster_up_to_date, patients.size),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.roster_up_to_date_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        // Desplegada, el chevron apunta hacia arriba (se pliega al tocar).
        Icon(
            imageVector = HealthifyIcons.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.rotate(if (expanded) CHEVRON_UP else 0f),
        )
    }
}

@Composable
private fun Avatar(name: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(HealthifyTheme.dimens.borderThin, scheme.surfaceContainerLowest, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
    }
}

/** El fondo del avatar acompaña al motivo: señal (naranja claro), consulta (verde claro), lo demás neutro. */
@Composable
private fun RosterAttention?.avatarColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        RosterAttention.OPEN_REVIEW -> scheme.secondaryContainer
        RosterAttention.CONSULTATION_IN_PROGRESS -> scheme.primaryContainer
        else -> scheme.surfaceContainer
    }
}

private fun RosterAttention.labelRes(): Int = when (this) {
    RosterAttention.OPEN_REVIEW -> R.string.roster_reason_open_review
    RosterAttention.CONSULTATION_IN_PROGRESS -> R.string.roster_reason_consultation
    RosterAttention.MISSING_BASELINE -> R.string.roster_reason_baseline
    RosterAttention.PLAN_NOT_PUBLISHED -> R.string.roster_reason_plan
}

private fun previewPatient(id: Long, name: String, attention: RosterAttention? = null) =
    RosterItem(id, id + 10, name, Instant.parse("2026-03-12T15:00:00Z"), isNew = false, attention = attention)

private val previewPatients = listOf(
    previewPatient(1, "Ana Flores", RosterAttention.OPEN_REVIEW),
    previewPatient(2, "Carlos Quispe", RosterAttention.CONSULTATION_IN_PROGRESS),
    previewPatient(3, "Luz Ramírez", RosterAttention.MISSING_BASELINE),
    previewPatient(4, "Jorge Salas"),
    previewPatient(5, "María Torres"),
    previewPatient(6, "Rosa Vega"),
)

@Preview(name = "PR1 · Mis pacientes (agrupada por acción)", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterPreview() {
    HealthifyTheme {
        PatientRosterContent(state = PatientRosterUiState(isLoading = false, patients = previewPatients), actions = PatientRosterActions())
    }
}

@Preview(name = "PR1 · «Al día» desplegada", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterExpandedPreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, patients = previewPatients, isUpToDateExpanded = true),
            actions = PatientRosterActions(),
        )
    }
}

@Preview(name = "PR1 · Todos al día", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterAllUpToDatePreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, patients = previewPatients.map { it.copy(attention = null) }),
            actions = PatientRosterActions(),
        )
    }
}

@Preview(name = "PR1 · Sin resultados de búsqueda", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterNoResultsPreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, patients = previewPatients, query = "Zoila"),
            actions = PatientRosterActions(),
        )
    }
}

@Preview(name = "PR1.L · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterLoadingPreview() {
    HealthifyTheme { PatientRosterContent(state = PatientRosterUiState(), actions = PatientRosterActions()) }
}

@Preview(name = "PR1.V · Sin pacientes", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterEmptyPreview() {
    HealthifyTheme {
        PatientRosterContent(state = PatientRosterUiState(isLoading = false, patients = emptyList()), actions = PatientRosterActions())
    }
}

@Preview(name = "PR1.O · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterOfflinePreview() {
    HealthifyTheme {
        PatientRosterContent(
            state = PatientRosterUiState(isLoading = false, isOffline = true, loadFailed = true),
            actions = PatientRosterActions(),
        )
    }
}

@Preview(name = "PR1 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRosterErrorPreview() {
    HealthifyTheme {
        PatientRosterContent(state = PatientRosterUiState(isLoading = false, loadFailed = true), actions = PatientRosterActions())
    }
}
