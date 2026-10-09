package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonChart
import pe.edu.upc.healthify.core.designsystem.component.SkeletonLine
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.isToday
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrendPoint
import pe.edu.upc.healthify.features.intake.presentation.components.WeightTrendChart
import pe.edu.upc.healthify.features.intake.presentation.components.numberText
import pe.edu.upc.healthify.features.intake.presentation.state.WeeklyCard
import pe.edu.upc.healthify.features.intake.presentation.state.WeightTrendEvent
import pe.edu.upc.healthify.features.intake.presentation.state.WeightTrendUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.WeightTrendViewModel
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

data class WeightTrendActions(
    val onRegisterWeighIn: () -> Unit = {},
    val onOpenWeeklySummary: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** PT13 · Tendencia de peso (+ PT13.1, PT13.V, PT13.L). Vive en la pestaña «Progreso» del shell. */
@Composable
fun WeightTrendScreen(
    onRegisterWeighIn: () -> Unit,
    onOpenWeeklySummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeightTrendViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is WeightTrendEvent.ShowSaved -> scope.launch {
                snackbarHostState.showSnackbar(
                    resources.getString(
                        when (event.outcome) {
                            SelfWeighInOutcome.RECORDED -> R.string.weight_trend_snackbar_saved
                            SelfWeighInOutcome.QUEUED -> R.string.weight_trend_snackbar_queued
                        },
                    ),
                )
            }
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    WeightTrendContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = WeightTrendActions(
            onRegisterWeighIn = onRegisterWeighIn,
            onOpenWeeklySummary = onOpenWeeklySummary,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

@Composable
fun WeightTrendContent(
    state: WeightTrendUiState,
    actions: WeightTrendActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.weight_trend_title))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                if (state.isOffline || state.cachedAt != null) OfflineNotice(state.cachedAt, state.isOffline)
                state.weeklyCard?.let { card -> WeeklySummaryCard(card = card, onClick = actions.onOpenWeeklySummary) }
                val trend = state.trend
                when {
                    state.isLoading -> TrendSkeleton()
                    trend != null -> TrendSection(trend = trend, weeks = state.weeks, showExcluded = state.showExcluded)
                    state.isEmpty -> EmptyState(
                        title = stringResource(R.string.weight_trend_empty_title),
                        text = stringResource(R.string.weight_trend_empty_body),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.space64),
                    )
                    state.isOfflineWithoutCopy -> EmptyState(
                        title = stringResource(R.string.weight_trend_offline_title),
                        text = stringResource(R.string.weight_trend_offline_body),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.space64),
                    )
                    state.loadFailed -> ErrorState(
                        onRetry = actions.onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.space24),
                    )
                }
            }
            HealthifySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = dimens.space16, vertical = dimens.space8),
            )
        }
        if (!state.isLoading) {
            // «Registrar autopesaje»: siempre a mano (también sin conexión: se encola).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(dimens.space16),
            ) {
                HealthifyButton(
                    text = stringResource(R.string.weight_trend_register),
                    onClick = actions.onRegisterWeighIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OfflineNotice(cachedAt: Instant?, isOffline: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        if (isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
        if (cachedAt != null) {
            val time = if (cachedAt.isToday()) {
                cachedAt.shortTimeText()
            } else {
                stringResource(R.string.format_separator_dot, cachedAt.mediumDateText(), cachedAt.shortTimeText())
            }
            Text(
                text = stringResource(R.string.weight_trend_cached_at, time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** «Card · Tu semana (IA)»: contorno `tertiary`, insignia, titular de la IA tal cual y «Ver resumen de la semana ›». */
@Composable
private fun WeeklySummaryCard(card: WeeklyCard, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = scheme.tertiary,
    ) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            AiBadge(label = stringResource(R.string.weekly_card_badge))
            Text(
                text = stringResource(R.string.weekly_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = when (card) {
                    is WeeklyCard.Ready -> card.headline
                    WeeklyCard.NotYet -> stringResource(R.string.weekly_empty_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
                Text(
                    text = stringResource(R.string.weekly_card_action),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurface)
            }
        }
    }
}

/** «Promedio de tus últimos autopesajes» + card con el gráfico, la explicación y el aviso de excluidos. */
@Composable
private fun TrendSection(trend: WeightTrend, weeks: Int, showExcluded: Boolean) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val points = trend.rangePoints
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = stringResource(R.string.weight_trend_section),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
            ) {
                WeightTrendChart(
                    points = points,
                    from = trend.rangeFrom ?: points.first().date,
                    to = trend.rangeTo ?: points.last().date,
                )
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    Text(
                        text = trendExplanation(trend, weeks),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    if (showExcluded) {
                        Text(
                            text = stringResource(R.string.weight_trend_excluded),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * «Promedio móvil de 7 autopesajes que siguieron el protocolo. Resumen: subió 0,6 kg en 4 semanas.»
 *
 * DECISIÓN PT13: el frame dice «Promedio móvil de 7 días», pero `windowSize` del backend cuenta autopesajes, no
 * días; el texto dice lo que el número es. El resumen sale de `changeKgOverRange` (sobre la serie suavizada, IN-5).
 */
@Composable
private fun trendExplanation(trend: WeightTrend, weeks: Int): String {
    val window = pluralStringResource(R.plurals.weight_trend_window, trend.windowSize, trend.windowSize)
    val change = trend.changeKgOverRange ?: return window
    val summary = when (trend.direction) {
        WeightTrend.Direction.UP -> pluralStringResource(R.plurals.weight_trend_summary_up, weeks, weeks, numberText(abs(change)))
        WeightTrend.Direction.DOWN -> pluralStringResource(R.plurals.weight_trend_summary_down, weeks, weeks, numberText(abs(change)))
        WeightTrend.Direction.STABLE, null -> pluralStringResource(R.plurals.weight_trend_summary_stable, weeks, weeks)
    }
    return "$window $summary"
}

/** PT13.L: skeleton sobre el gráfico y dos líneas. */
@Composable
private fun TrendSkeleton() {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        SkeletonChart()
        SkeletonLine()
    }
}

private val PreviewTrend = run {
    val from = LocalDate.parse("2026-08-15")
    val values = listOf(67.3, 67.5, 67.6, 67.55, 67.5, 67.5, 67.8, 68.2, 68.4, 68.5, 68.6, 68.9)
    WeightTrend(
        points = values.mapIndexed { index, kg -> WeightTrendPoint(from.plusDays(index * 2L + 1), kg) },
        windowSize = 7,
        lastRecalculatedAt = Instant.parse("2026-09-12T12:00:00Z"),
        excludedReadingsCount = 2,
        changeKgOverRange = 0.6,
        slopeKgPerWeek = 0.15,
        rangeFrom = from,
        rangeTo = LocalDate.parse("2026-09-12"),
    )
}

@Preview(name = "PT13 · Tendencia de peso", widthDp = 360, heightDp = 800)
@Composable
private fun WeightTrendPreview() {
    HealthifyTheme {
        WeightTrendContent(
            state = WeightTrendUiState(
                isLoading = false,
                trend = PreviewTrend,
                weeklyCard = WeeklyCard.Ready("Cumpliste tus metas 5 de 7 días y registraste casi todas tus comidas."),
                weeks = 4,
            ),
            actions = WeightTrendActions(),
        )
    }
}

@Preview(name = "PT13.V · Aún sin tendencia", widthDp = 360, heightDp = 800)
@Composable
private fun WeightTrendEmptyPreview() {
    HealthifyTheme {
        WeightTrendContent(state = WeightTrendUiState(isLoading = false, hasNoTrend = true, weeks = 4), actions = WeightTrendActions())
    }
}

@Preview(name = "PT13.L · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun WeightTrendLoadingPreview() {
    HealthifyTheme { WeightTrendContent(state = WeightTrendUiState(weeks = 4), actions = WeightTrendActions()) }
}

@Preview(name = "PT13 · Sin conexión (copia del teléfono)", widthDp = 360, heightDp = 800)
@Composable
private fun WeightTrendOfflinePreview() {
    HealthifyTheme {
        WeightTrendContent(
            state = WeightTrendUiState(
                isLoading = false,
                isOffline = true,
                trend = PreviewTrend.copy(fromCache = true, savedAt = Instant.parse("2026-09-12T13:10:00Z"), excludedReadingsCount = 0),
                weeks = 4,
            ),
            actions = WeightTrendActions(),
        )
    }
}

@Preview(name = "PT13 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun WeightTrendErrorPreview() {
    HealthifyTheme {
        WeightTrendContent(
            state = WeightTrendUiState(isLoading = false, loadFailed = true, weeklyCard = WeeklyCard.NotYet, weeks = 4),
            actions = WeightTrendActions(),
        )
    }
}
