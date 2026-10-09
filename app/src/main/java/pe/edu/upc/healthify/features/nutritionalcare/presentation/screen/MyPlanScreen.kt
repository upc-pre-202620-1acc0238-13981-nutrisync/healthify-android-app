package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.presentation.components.CachedTargetsNotice
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanDetails
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MyPlanEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MyPlanUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanChangeLine
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.MyPlanViewModel
import java.time.Instant

// Medidas del frame «PT4 · Mi plan»: barras de macronutrientes de 6 dp.
private val MacroBarHeight = 6.dp

/** Acciones de PT4 que la pantalla con estado conecta con el ViewModel. */
data class MyPlanActions(
    val onBack: () -> Unit = {},
    val onOpenVersions: () -> Unit = {},
    val onAcknowledge: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onServerErrorRetry: () -> Unit = {},
    val onServerErrorDismiss: () -> Unit = {},
)

/** PT4 · Mi plan: metas, indicaciones, restricciones, «Qué cambió» y el mensaje de la versión vigente. */
@Composable
fun MyPlanScreen(
    onBack: () -> Unit,
    onOpenVersions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            MyPlanEvent.NavigateBack -> onBack()
        }
    }
    MyPlanContent(
        state = state,
        actions = MyPlanActions(
            onBack = onBack,
            onOpenVersions = onOpenVersions,
            onAcknowledge = viewModel::onAcknowledge,
            onRetry = viewModel::onRetry,
            onServerErrorRetry = viewModel::onServerErrorRetry,
            onServerErrorDismiss = viewModel::onServerErrorDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun MyPlanContent(
    state: MyPlanUiState,
    actions: MyPlanActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.my_plan_title), onBack = actions.onBack)
        val plan = state.plan
        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                repeat(3) { SkeletonCard() }
            }
            plan != null -> PlanBody(
                plan = plan,
                isOffline = state.isOffline,
                cachedAt = state.cachedAt,
                isAcknowledging = state.isAcknowledging,
                actions = actions,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
                val stateModifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                if (state.hasNoTargets || state.isOffline) {
                    // Nota PT3 «Error cache → mismo copy que Vacío»; sin conexión y sin copia, igual.
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        text = stringResource(R.string.home_empty_body),
                        modifier = stateModifier,
                    )
                } else {
                    ErrorState(onRetry = actions.onRetry, modifier = stateModifier)
                }
            }
        }
    }
    if (state.showServerError) {
        ServerErrorDialog(onRetry = actions.onServerErrorRetry, onDismiss = actions.onServerErrorDismiss)
    }
}

@Composable
private fun PlanBody(
    plan: PlanDetails,
    isOffline: Boolean,
    cachedAt: Instant?,
    isAcknowledging: Boolean,
    actions: MyPlanActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space24),
    ) {
        if (isOffline || cachedAt != null) CachedTargetsNotice(cachedAt = cachedAt)

        // Card · Plan vigente
        PlanCard(verticalSpacing = dimens.space4) {
            Text(
                text = stringResource(R.string.my_plan_version_current, plan.planVersion),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.my_plan_published, plan.publishedAt.mediumDateText()),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimens.touchMin)
                    .clickable(role = Role.Button, onClick = actions.onOpenVersions),
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(HealthifyIcons.History, contentDescription = null, tint = scheme.onSurface)
                Text(
                    text = stringResource(R.string.my_plan_previous_versions),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurface)
            }
        }

        PlanSection(title = stringResource(R.string.my_plan_daily_targets)) {
            PlanCard(verticalSpacing = dimens.space8) {
                Text(
                    text = stringResource(R.string.format_kcal, UiText.formatNumber(plan.energyKcal, locale)),
                    style = HealthifyTheme.extendedTypography.metricMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.my_plan_macros,
                        UiText.formatNumber(plan.proteinG, locale),
                        UiText.formatNumber(plan.carbG, locale),
                        UiText.formatNumber(plan.fatG, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                // DECISIÓN PT4: cada barra es la parte de la energía diaria que aporta ese macronutriente.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                ) {
                    MacroBar(plan.proteinShare, scheme.secondary, Modifier.weight(1f))
                    MacroBar(plan.carbShare, scheme.tertiary, Modifier.weight(1f))
                    MacroBar(plan.fatShare, scheme.primary, Modifier.weight(1f))
                }
            }
        }

        if (plan.guidelines.isNotEmpty()) {
            PlanSection(title = stringResource(R.string.my_plan_guidelines)) {
                PlanCard(verticalSpacing = dimens.space12) {
                    plan.guidelines.forEach { IconLine(icon = HealthifyIcons.Check, text = it.asString()) }
                }
            }
        }

        if (plan.restrictions.isNotEmpty()) {
            PlanSection(title = stringResource(R.string.my_plan_restrictions)) {
                PlanCard(verticalSpacing = dimens.space12) {
                    plan.restrictions.forEach { IconLine(icon = HealthifyIcons.Close, text = it.asString()) }
                }
            }
        }

        if (plan.changes.isNotEmpty()) {
            PlanSection(title = stringResource(R.string.my_plan_changes)) {
                PlanCard(verticalSpacing = dimens.space4) {
                    plan.changes.forEach { ChangeLine(it) }
                }
            }
        }

        // DECISIÓN PT4: el frame no dibuja el mensaje de NC-9; va como una sección más, con el texto tal cual.
        plan.patientMessage?.let { message ->
            PlanSection(title = stringResource(R.string.my_plan_message)) {
                PlanCard(verticalSpacing = dimens.space4) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
            }
        }

        HealthifyButton(
            text = stringResource(R.string.my_plan_acknowledge),
            onClick = actions.onAcknowledge,
            loading = isAcknowledging,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        Text(
            text = title,
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun PlanCard(verticalSpacing: Dp, content: @Composable ColumnScope.() -> Unit) {
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(HealthifyTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String) {
    val dimens = HealthifyTheme.dimens
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(dimens.icon))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChangeLine(line: PlanChangeLine) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = line.text.asString(),
        style = if (line.secondary) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = if (line.secondary) scheme.onSurfaceVariant else scheme.onSurface,
    )
}

@Composable
private fun MacroBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(HealthifyTheme.dimens.radiusSm)
    Box(
        modifier = modifier
            .height(MacroBarHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(shape)
                .background(color),
        )
    }
}

internal fun previewPlan() = PlanDetails(
    planVersion = 3,
    publishedAt = Instant.parse("2026-09-04T15:00:00Z"),
    energyKcal = 1850.0,
    proteinG = 90.0,
    carbG = 238.0,
    fatG = 60.0,
    proteinShare = 0.19f,
    carbShare = 0.51f,
    fatShare = 0.29f,
    guidelines = listOf(
        UiText.of(R.string.guideline_prioritize_vegetables),
        UiText.of(R.string.guideline_avoid_sugary_drinks),
        UiText.of(R.string.guideline_reduce_salt),
    ),
    restrictions = listOf(UiText.of(R.string.restriction_shellfish_free), UiText.Raw("Sin ají")),
    changes = listOf(
        PlanChangeLine(UiText.of(R.string.plan_change_guideline_added, UiText.of(R.string.guideline_reduce_salt)), false),
        PlanChangeLine(UiText.of(R.string.plan_change_no_target_changes), true),
    ),
    patientMessage = "Notamos que tus cenas son más ligeras. Probemos con estas ideas.",
)

@Preview(name = "PT4 · Mi plan", widthDp = 360, heightDp = 800)
@Composable
private fun MyPlanPreview() {
    HealthifyTheme {
        MyPlanContent(state = MyPlanUiState(isLoading = false, plan = previewPlan()), actions = MyPlanActions())
    }
}

@Preview(name = "PT4 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun MyPlanLoadingPreview() {
    HealthifyTheme { MyPlanContent(state = MyPlanUiState(), actions = MyPlanActions()) }
}

@Preview(name = "PT4 · Sin conexión (copia del teléfono)", widthDp = 360, heightDp = 800)
@Composable
private fun MyPlanOfflinePreview() {
    HealthifyTheme {
        MyPlanContent(
            state = MyPlanUiState(isLoading = false, isOffline = true, plan = previewPlan(), cachedAt = Instant.now()),
            actions = MyPlanActions(),
        )
    }
}

@Preview(name = "PT4 · Sin metas", widthDp = 360, heightDp = 800)
@Composable
private fun MyPlanEmptyPreview() {
    HealthifyTheme {
        MyPlanContent(state = MyPlanUiState(isLoading = false, hasNoTargets = true), actions = MyPlanActions())
    }
}

@Preview(name = "PT4 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun MyPlanErrorPreview() {
    HealthifyTheme {
        MyPlanContent(state = MyPlanUiState(isLoading = false, loadFailed = true), actions = MyPlanActions())
    }
}
