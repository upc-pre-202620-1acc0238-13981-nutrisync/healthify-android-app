package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionItem
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanVersionsUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PlanVersionsViewModel
import java.time.Instant

/** PT4.1 · Versiones anteriores del plan, solo de consulta. */
@Composable
fun PlanVersionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanVersionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlanVersionsContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun PlanVersionsContent(
    state: PlanVersionsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.plan_versions_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline && state.versions.isEmpty()) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            when {
                state.isLoading -> repeat(3) { SkeletonListItem() }
                state.versions.isNotEmpty() -> {
                    VersionsCard(versions = state.versions)
                    Text(
                        text = stringResource(R.string.plan_versions_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                state.isOffline -> EmptyState(
                    title = stringResource(R.string.plan_versions_offline_title),
                    text = stringResource(R.string.plan_versions_offline_body),
                )
                state.loadFailed -> ErrorState(onRetry = onRetry)
                else -> EmptyState(
                    title = stringResource(R.string.plan_versions_empty_title),
                    text = stringResource(R.string.plan_versions_empty_body),
                )
            }
        }
    }
}

@Composable
private fun VersionsCard(versions: List<PlanVersionItem>) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            versions.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(thickness = dimens.borderThin, color = scheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                        Text(
                            text = stringResource(R.string.plan_versions_item, item.version),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.format_separator_dot,
                                item.publishedAt.mediumDateText(),
                                stringResource(R.string.format_kcal, UiText.formatNumber(item.energyKcal, locale)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (item.isActive) {
                        StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.plan_versions_current))
                    }
                }
            }
        }
    }
}

private val previewVersions = listOf(
    PlanVersionItem(3, Instant.parse("2026-09-04T15:00:00Z"), 1850.0, isActive = true),
    PlanVersionItem(2, Instant.parse("2026-06-12T15:00:00Z"), 1900.0, isActive = false),
    PlanVersionItem(1, Instant.parse("2026-03-12T15:00:00Z"), 2000.0, isActive = false),
)

@Preview(name = "PT4.1 · Versiones anteriores", widthDp = 360, heightDp = 800)
@Composable
private fun PlanVersionsPreview() {
    HealthifyTheme {
        PlanVersionsContent(
            state = PlanVersionsUiState(isLoading = false, versions = previewVersions),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PT4.1 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PlanVersionsLoadingPreview() {
    HealthifyTheme { PlanVersionsContent(state = PlanVersionsUiState(), onBack = {}, onRetry = {}) }
}

@Preview(name = "PT4.1 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PlanVersionsOfflinePreview() {
    HealthifyTheme {
        PlanVersionsContent(state = PlanVersionsUiState(isLoading = false, isOffline = true), onBack = {}, onRetry = {})
    }
}

@Preview(name = "PT4.1 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun PlanVersionsErrorPreview() {
    HealthifyTheme {
        PlanVersionsContent(state = PlanVersionsUiState(isLoading = false, loadFailed = true), onBack = {}, onRetry = {})
    }
}
