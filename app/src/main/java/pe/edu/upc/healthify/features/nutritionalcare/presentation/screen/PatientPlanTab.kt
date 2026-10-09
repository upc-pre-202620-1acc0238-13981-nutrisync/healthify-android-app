package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTag
import pe.edu.upc.healthify.core.designsystem.component.IconInfoRow
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NutritionPlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanRestrictionItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.compactText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.shortDateText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.text
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientPlanUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PatientPlanViewModel
import java.time.Instant
import java.time.ZoneId

/**
 * PAC-4 · pestaña Plan: metas vigentes, si el paciente las vio («Ana las vio el mismo día», `lastAcknowledgedAt` de
 * CR-3), indicaciones, restricciones e historial. «Ajustar plan» inicia o reanuda la consulta guiada.
 *
 * DECISIÓN PAC-4: las filas del historial no son táctiles (no hay frame de detalle de una versión).
 */
@Composable
fun PatientPlanTab(
    patientId: Long,
    patientName: String,
    onNavigate: (route: Any) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    ObserveEvents(viewModel.events) { event -> onNavigate(event.toRoute(patientId, patientName)) }
    PatientPlanContent(
        state = state,
        patientName = patientName,
        onAdjustPlan = viewModel::onAdjustPlan,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
    if (state.showStartError) {
        ServerErrorDialog(onRetry = viewModel::onAdjustPlan, onDismiss = viewModel::onStartErrorDismissed)
    }
}

@Composable
fun PatientPlanContent(
    state: PatientPlanUiState,
    patientName: String,
    onAdjustPlan: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val history = state.history
    PatientTabLayout(
        modifier = modifier,
        bottomBar = history?.active?.let {
            {
                HealthifyButton(
                    text = stringResource(R.string.plan_adjust),
                    onClick = onAdjustPlan,
                    style = HealthifyButtonStyle.Tonal,
                    loading = state.isStartingConsultation,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        when {
            history != null && history.active == null -> EmptyState(
                title = stringResource(R.string.plan_empty_title),
                text = stringResource(R.string.plan_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
            history != null -> PlanBody(history, state.readStatus, patientName)
            state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
            state.loadFailed && state.isOffline -> NeedsConnection(onRetry)
            state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val SKELETON_ROWS = 3

@Composable
private fun ColumnScope.PlanBody(history: PlanHistory, readStatus: TargetsReadStatus?, patientName: String) {
    val active = history.active ?: return
    ActiveTargetsCard(active, readStatus, patientName)
    GuidelinesCard(active)
    HistorySection(history.versions)
}

@Composable
private fun ActiveTargetsCard(active: NutritionPlanVersion, readStatus: TargetsReadStatus?, patientName: String) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.plan_active_targets),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.plan_version_chip, active.version))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            MacroColumn(compactText(active.targets.energyKcal, 0), stringResource(R.string.plan_kcal_per_day), Modifier.weight(1f))
            MacroColumn(gramsText(active.targets.proteinG), stringResource(R.string.macro_protein), Modifier.weight(1f))
            MacroColumn(gramsText(active.targets.carbG), stringResource(R.string.macro_carbs_short), Modifier.weight(1f))
            MacroColumn(gramsText(active.targets.fatG), stringResource(R.string.macro_fat), Modifier.weight(1f))
        }
        Text(
            text = publishedLine(active, readStatus, patientName),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroColumn(value: String, label: String, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun gramsText(grams: Double): String = stringResource(R.string.unit_g_value, compactText(grams, 0))

/**
 * «Publicadas el 4 sept. · Ana las vio el mismo día.» Con la versión vigente acusada se dice cuándo; si aún no la vio,
 * «todavía no las revisa» (tono de invitación). Sin lectura del vínculo solo va la fecha.
 */
@Composable
private fun publishedLine(active: NutritionPlanVersion, readStatus: TargetsReadStatus?, patientName: String): String {
    val zone = ZoneId.systemDefault()
    val published = active.publishedAt.atZone(zone).toLocalDate()
    val publishedText = stringResource(R.string.plan_published_on, shortDateText(published))
    val firstName = patientName.trim().substringBefore(' ').ifBlank { patientName }
    val seen = when {
        readStatus == null -> null
        readStatus.lastAcknowledgedVersion == active.version && readStatus.lastAcknowledgedAt != null -> {
            val seenOn = readStatus.lastAcknowledgedAt.atZone(zone).toLocalDate()
            if (seenOn == published) {
                stringResource(R.string.plan_seen_same_day, firstName)
            } else {
                stringResource(R.string.plan_seen_on, firstName, shortDateText(seenOn))
            }
        }
        readStatus.lastAcknowledgedVersion == active.version -> stringResource(R.string.plan_seen, firstName)
        else -> stringResource(R.string.plan_not_seen_yet, firstName)
    }
    return listOfNotNull(publishedText, seen).joinToString(stringResource(R.string.text_separator))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuidelinesCard(active: NutritionPlanVersion) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(verticalSpacing = dimens.space16) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Text(text = stringResource(R.string.plan_guidelines), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (active.guidelines.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                active.guidelines.forEach { HealthifyTag(text = it.text()) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Text(text = stringResource(R.string.plan_restrictions), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (active.restrictions.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                active.restrictions.forEach { HealthifyTag(text = it.text()) }
            }
        }
    }
}

/** «HISTORIAL DEL PLAN»: «Versión 3 · 4 sept. 2026 · 1 796 kcal · Vigente». */
@Composable
private fun HistorySection(versions: List<NutritionPlanVersion>) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        SectionOverline(stringResource(R.string.plan_history))
        SectionCard {
            versions.forEachIndexed { index, version ->
                if (index > 0) CardDivider()
                IconInfoRow(
                    icon = HealthifyIcons.History,
                    title = stringResource(R.string.plan_version_title, version.version),
                    supportingText = stringResource(
                        R.string.plan_version_line,
                        version.publishedAt.mediumDateText(),
                        compactText(version.targets.energyKcal, 0),
                    ),
                    trailing = if (version.isActive) {
                        { StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.patient_plan_active_chip)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private val previewHistory = PlanHistory.of(
    listOf(
        NutritionPlanVersion(
            version = 3,
            isActive = true,
            publishedAt = Instant.parse("2026-09-04T15:00:00Z"),
            targets = Targets(1796.0, 119.0, 195.0, 60.0),
            guidelines = listOf(
                PlanGuidelineItem.Catalog(PlanGuidelineCode.PRIORITIZE_VEGETABLES),
                PlanGuidelineItem.Catalog(PlanGuidelineCode.AVOID_SUGARY_DRINKS),
                PlanGuidelineItem.Custom("Caminar 20 minutos"),
            ),
            restrictions = listOf(PlanRestrictionItem.Catalog(PlanRestrictionCode.SHELLFISH_FREE)),
            patientMessage = null,
        ),
        NutritionPlanVersion(2, false, Instant.parse("2026-06-12T15:00:00Z"), Targets(1850.0, 110.0, 210.0, 62.0), emptyList(), emptyList(), null),
        NutritionPlanVersion(1, false, Instant.parse("2026-03-12T15:00:00Z"), Targets(1900.0, 110.0, 220.0, 63.0), emptyList(), emptyList(), null),
    ),
)

@Preview(name = "PAC-4 · Plan", widthDp = 360, heightDp = 800)
@Composable
private fun PatientPlanPreview() {
    HealthifyTheme {
        PatientPlanContent(
            state = PatientPlanUiState(
                isLoading = false,
                history = previewHistory,
                readStatus = TargetsReadStatus(
                    careLinkId = CareLinkId(4),
                    pendingVersion = null,
                    lastAcknowledgedVersion = 3,
                    hasPendingAcknowledgement = false,
                    lastAcknowledgedAt = Instant.parse("2026-09-04T20:00:00Z"),
                ),
            ),
            patientName = "Ana Flores",
            onAdjustPlan = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PAC-4 · Sin plan", widthDp = 360, heightDp = 800)
@Composable
private fun PatientPlanEmptyPreview() {
    HealthifyTheme {
        PatientPlanContent(
            state = PatientPlanUiState(isLoading = false, history = PlanHistory(emptyList())),
            patientName = "Luz Ramírez",
            onAdjustPlan = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PAC-4 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PatientPlanLoadingPreview() {
    HealthifyTheme {
        PatientPlanContent(state = PatientPlanUiState(), patientName = "Ana Flores", onAdjustPlan = {}, onRetry = {})
    }
}
