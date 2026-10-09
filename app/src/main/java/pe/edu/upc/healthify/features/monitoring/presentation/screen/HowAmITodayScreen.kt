package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.presentation.components.accentColor
import pe.edu.upc.healthify.features.monitoring.presentation.components.glyphRes
import pe.edu.upc.healthify.features.monitoring.presentation.components.messageRes
import pe.edu.upc.healthify.features.monitoring.presentation.state.HowAmITodayUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.HowAmITodayViewModel

// Medidas del frame «PT15 · Cómo voy hoy»: acento vertical de 6 dp.
private val AccentWidth = 6.dp

/** PT15 · Cómo voy hoy (+ PT15.O sin conexión). */
@Composable
fun HowAmITodayScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HowAmITodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HowAmITodayContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun HowAmITodayContent(
    state: HowAmITodayUiState,
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
        HealthifyTopAppBar(title = stringResource(R.string.today_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            val outcome = state.outcome
            when {
                state.isLoading -> SkeletonCard()
                outcome != null -> {
                    if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                    OutcomeCard(outcome)
                }
                state.isOffline -> {
                    OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                    EmptyState(
                        title = stringResource(R.string.today_offline_title),
                        text = stringResource(R.string.today_offline_body),
                    )
                }
                else -> ErrorState(onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun OutcomeCard(outcome: ComplianceOutcome) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
        ) {
            Box(
                modifier = Modifier
                    .width(AccentWidth)
                    .fillMaxHeight()
                    .background(outcome.accentColor()),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(
                    text = stringResource(outcome.glyphRes),
                    style = HealthifyTheme.extendedTypography.metricMedium,
                    color = scheme.primary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Text(
                    text = stringResource(outcome.messageRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

@Preview(name = "PT15 · Cómo voy hoy (Met)", widthDp = 360, heightDp = 800)
@Composable
private fun HowAmITodayPreview() {
    HealthifyTheme {
        HowAmITodayContent(
            state = HowAmITodayUiState(isLoading = false, outcome = ComplianceOutcome.MET),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PT15 · Exceeded / Short / Unlogged", widthDp = 360, heightDp = 800)
@Composable
private fun HowAmITodayOutcomesPreview() {
    HealthifyTheme {
        Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16)) {
            listOf(ComplianceOutcome.EXCEEDED, ComplianceOutcome.SHORT, ComplianceOutcome.UNLOGGED).forEach {
                OutcomeCard(it)
            }
        }
    }
}

@Preview(name = "PT15 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun HowAmITodayLoadingPreview() {
    HealthifyTheme { HowAmITodayContent(state = HowAmITodayUiState(), onBack = {}, onRetry = {}) }
}

@Preview(name = "PT15.O · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun HowAmITodayOfflinePreview() {
    HealthifyTheme {
        HowAmITodayContent(state = HowAmITodayUiState(isLoading = false, isOffline = true), onBack = {}, onRetry = {})
    }
}

@Preview(name = "PT15 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun HowAmITodayErrorPreview() {
    HealthifyTheme {
        HowAmITodayContent(state = HowAmITodayUiState(isLoading = false, loadFailed = true), onBack = {}, onRetry = {})
    }
}
