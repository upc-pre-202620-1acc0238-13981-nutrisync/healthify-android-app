package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryFacts
import pe.edu.upc.healthify.features.monitoring.presentation.state.WeeklySummaryUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.WeeklySummaryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** PT13.2 · Tu semana — resumen con IA (+ PT13.2.V aún sin resumen). Se abre desde la card de PT13. */
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklySummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeeklySummaryContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun WeeklySummaryContent(
    state: WeeklySummaryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
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
        HealthifyTopAppBar(title = stringResource(R.string.weekly_title), onBack = onBack)
        val summary = state.summary
        if (summary != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
                SummaryCard(summary)
                Text(
                    text = stringResource(R.string.weekly_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> SkeletonCard(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                state.notYet -> EmptyState(
                    title = stringResource(R.string.weekly_empty_title),
                    text = stringResource(R.string.weekly_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.isOff -> EmptyState(
                    title = stringResource(R.string.weekly_off_title),
                    text = stringResource(R.string.weekly_off_body),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadFailed && state.isOffline -> EmptyState(
                    title = stringResource(R.string.weekly_offline_title),
                    text = stringResource(R.string.weekly_offline_body),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** «Card · Resumen de la semana (IA)»: los textos de la IA van tal cual (ya llegan en el idioma del lector). */
@Composable
private fun SummaryCard(summary: WeeklySummary) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = scheme.tertiary,
    ) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
            AiBadge(label = stringResource(R.string.weekly_card_badge))
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(
                    text = weekRangeText(summary.weekStart, summary.weekEnd, currentLocale()),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = summary.headline, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (summary.wentWell.isNotEmpty()) {
                BulletBlock(
                    title = stringResource(R.string.weekly_went_well),
                    bullets = summary.wentWell,
                    icon = HealthifyIcons.Check,
                )
            }
            if (summary.watchOut.isNotEmpty()) {
                BulletBlock(
                    title = stringResource(R.string.weekly_watch_out),
                    bullets = summary.watchOut,
                    icon = HealthifyIcons.Info,
                )
            }
        }
    }
}

@Composable
private fun BulletBlock(title: String, bullets: List<String>, icon: ImageVector) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = title,
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = scheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        bullets.forEach { bullet ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.Top) {
                Icon(imageVector = icon, contentDescription = null, tint = scheme.onSurface)
                Text(
                    text = bullet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** «Semana del 8 al 14 de septiembre» · «Semana del 29 de septiembre al 5 de octubre» · «Week of September 8 to 14». */
internal fun weekRangeTextParts(start: LocalDate, end: LocalDate, locale: Locale): Pair<String, String> {
    val sameMonth = start.month == end.month && start.year == end.year
    val english = locale.language == Locale.ENGLISH.language
    val full = DateTimeFormatter.ofPattern(if (english) "MMMM d" else "d 'de' MMMM", locale)
    val dayOnly = DateTimeFormatter.ofPattern("d", locale)
    return if (english) {
        full.format(start) to (if (sameMonth) dayOnly else full).format(end)
    } else {
        (if (sameMonth) dayOnly else full).format(start) to full.format(end)
    }
}

@Composable
private fun weekRangeText(start: LocalDate, end: LocalDate, locale: Locale): String {
    val (from, to) = weekRangeTextParts(start, end, locale)
    return stringResource(R.string.weekly_week_range, from, to)
}

private val PreviewSummary = WeeklySummary(
    weekStart = LocalDate.parse("2026-09-08"),
    weekEnd = LocalDate.parse("2026-09-14"),
    headline = "Cumpliste tus metas 5 de 7 días.",
    wentWell = listOf(
        "Registraste tus comidas 6 de 7 días.",
        "Desayunaste con proteína casi todos los días.",
        "Tu peso bajó 0,3 kg esta semana.",
    ),
    watchOut = listOf("Las cenas suelen pasar tu meta de calorías.", "Los fines de semana registras menos."),
    facts = WeeklySummaryFacts(metDays = 5, totalDays = 7, loggedDays = 6, unloggedDays = 1, weightChangeKg = -0.3),
    generatedAt = Instant.parse("2026-09-15T11:00:00Z"),
)

@Preview(name = "PT13.2 · Tu semana", widthDp = 360, heightDp = 800)
@Composable
private fun WeeklySummaryPreview() {
    HealthifyTheme {
        WeeklySummaryContent(WeeklySummaryUiState(isLoading = false, summary = PreviewSummary), onBack = {}, onRetry = {})
    }
}

@Preview(name = "PT13.2.V · Aún sin resumen", widthDp = 360, heightDp = 800)
@Composable
private fun WeeklySummaryEmptyPreview() {
    HealthifyTheme { WeeklySummaryContent(WeeklySummaryUiState(isLoading = false, notYet = true), onBack = {}, onRetry = {}) }
}

@Preview(name = "PT13.2 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun WeeklySummaryLoadingPreview() {
    HealthifyTheme { WeeklySummaryContent(WeeklySummaryUiState(), onBack = {}, onRetry = {}) }
}

@Preview(name = "PT13.2 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun WeeklySummaryOfflinePreview() {
    HealthifyTheme {
        WeeklySummaryContent(
            WeeklySummaryUiState(isLoading = false, isOffline = true, loadFailed = true),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PT13.2 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun WeeklySummaryErrorPreview() {
    HealthifyTheme { WeeklySummaryContent(WeeklySummaryUiState(isLoading = false, loadFailed = true), onBack = {}, onRetry = {}) }
}
