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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.presentation.components.NextConsultationCard
import pe.edu.upc.healthify.features.monitoring.presentation.components.toUiText
import pe.edu.upc.healthify.features.monitoring.presentation.state.FollowUpPreparationUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.FollowUpPreparationViewModel

/** PT25.1 · Tu consulta — cómo prepararte (+ PT25.1.V sin indicaciones). Se llega desde «Próxima consulta» de PT3. */
@Composable
fun FollowUpPreparationScreen(
    onBack: () -> Unit,
    onOpenMyConsultations: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpPreparationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    FollowUpPreparationContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onAddToCalendar = rememberAddToCalendarHandler(snackbarHostState),
        onOpenMyConsultations = onOpenMyConsultations,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun FollowUpPreparationContent(
    state: FollowUpPreparationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddToCalendar: (NextFollowUp) -> Unit,
    onOpenMyConsultations: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.follow_up_preparation_title), onBack = onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                val next = state.next
                when {
                    next != null -> {
                        NextConsultationCard(next = next, onAddToCalendar = { onAddToCalendar(next) })
                        PreparationSection(next.preparation)
                    }
                    state.isLoading -> SkeletonCard(modifier = Modifier.fillMaxWidth())
                    state.noConsultation -> EmptyState(
                        title = stringResource(R.string.consultations_none_title),
                        text = stringResource(R.string.consultations_none_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.loadFailed && state.isOffline -> EmptyState(
                        title = stringResource(R.string.consultations_offline_title),
                        text = stringResource(R.string.consultations_offline_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
                }
                if (!state.isLoading) {
                    HealthifyButton(
                        text = stringResource(R.string.follow_up_preparation_see_consultations),
                        onClick = onOpenMyConsultations,
                        style = HealthifyButtonStyle.Text,
                        modifier = Modifier.fillMaxWidth(),
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
    }
}

/** «CÓMO PREPARARTE»: las indicaciones traducidas en la app; sin ninguna, PT25.1.V. */
@Composable
private fun PreparationSection(preparation: List<PreparationInstruction>) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = stringResource(R.string.follow_up_preparation_section),
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = scheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (preparation.isEmpty()) {
            Column(
                modifier = Modifier.padding(vertical = dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                Text(
                    text = stringResource(R.string.follow_up_preparation_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.follow_up_preparation_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            return@Column
        }
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                preparation.forEach { instruction ->
                    Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = HealthifyIcons.Check, contentDescription = null, tint = scheme.primary)
                        Text(
                            text = instruction.toUiText().asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.follow_up_preparation_note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "PT25.1 · Cómo prepararte", widthDp = 360, heightDp = 800)
@Composable
private fun FollowUpPreparationPreview() {
    HealthifyTheme {
        FollowUpPreparationContent(
            state = FollowUpPreparationUiState(
                isLoading = false,
                next = PreviewNextFollowUp.copy(
                    preparation = listOf(
                        PreparationInstruction.Catalog(PreparationCode.FASTING),
                        PreparationInstruction.Catalog(PreparationCode.LIGHT_CLOTHING),
                        PreparationInstruction.Catalog(PreparationCode.BRING_BLOOD_TESTS),
                    ),
                ),
            ),
            onBack = {},
            onRetry = {},
            onAddToCalendar = {},
            onOpenMyConsultations = {},
        )
    }
}

@Preview(name = "PT25.1.V · Sin indicaciones", widthDp = 360, heightDp = 800)
@Composable
private fun FollowUpPreparationEmptyPreview() {
    HealthifyTheme {
        FollowUpPreparationContent(
            state = FollowUpPreparationUiState(isLoading = false, next = PreviewNextFollowUp),
            onBack = {},
            onRetry = {},
            onAddToCalendar = {},
            onOpenMyConsultations = {},
        )
    }
}

@Preview(name = "PT25.1 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun FollowUpPreparationLoadingPreview() {
    HealthifyTheme {
        FollowUpPreparationContent(
            state = FollowUpPreparationUiState(),
            onBack = {},
            onRetry = {},
            onAddToCalendar = {},
            onOpenMyConsultations = {},
        )
    }
}

@Preview(name = "PT25.1 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun FollowUpPreparationOfflinePreview() {
    HealthifyTheme {
        FollowUpPreparationContent(
            state = FollowUpPreparationUiState(isLoading = false, isOffline = true, loadFailed = true),
            onBack = {},
            onRetry = {},
            onAddToCalendar = {},
            onOpenMyConsultations = {},
        )
    }
}
