package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifySwitchRow
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.presentation.state.AiFeaturesUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentOffer
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.AiFeaturesEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.AiFeaturesViewModel

data class AiFeaturesActions(
    val onBack: () -> Unit,
    val onToggle: (AiFeature, Boolean) -> Unit = { _, _ -> },
    val onActivateConsent: () -> Unit = {},
    val onConfirmConsent: () -> Unit = {},
    val onDismissConsent: () -> Unit = {},
)

/** PT21.IA · Ajustes — Funciones con IA. Se llega desde PT21 «Funciones con IA». */
@Composable
fun AiFeaturesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiFeaturesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is AiFeaturesEvent.ShowSaveFailed -> scope.launch {
                snackbarHostState.showSnackbar(
                    resources.getString(if (event.offline) R.string.ai_features_save_offline else R.string.ai_features_save_failed),
                )
            }
        }
    }
    AiFeaturesContent(
        state = state,
        actions = AiFeaturesActions(
            onBack = onBack,
            onToggle = viewModel::onToggle,
            onActivateConsent = viewModel::onActivateConsent,
            onConfirmConsent = viewModel::onConfirmConsent,
            onDismissConsent = viewModel::onDismissConsent,
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun AiFeaturesContent(
    state: AiFeaturesUiState,
    actions: AiFeaturesActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
        HealthifyTopAppBar(title = stringResource(R.string.ai_features_title), onBack = actions.onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                Text(
                    text = stringResource(R.string.ai_features_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                if (state.isLoading) {
                    SkeletonCard(modifier = Modifier.fillMaxWidth())
                } else {
                    if (!state.consentGranted) ConsentCard(state, actions.onActivateConsent)
                    FeaturesCard(state, actions.onToggle)
                    Text(
                        text = stringResource(R.string.ai_features_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
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
    if (state.consentOffer != null) {
        HealthifyDialog(
            title = stringResource(R.string.ai_features_consent_dialog_title),
            text = stringResource(R.string.consent_ai_what),
            confirmLabel = stringResource(R.string.ai_features_consent_dialog_confirm),
            onConfirm = actions.onConfirmConsent,
            onDismiss = actions.onDismissConsent,
            dismissLabel = stringResource(R.string.ai_features_consent_dialog_dismiss),
            confirmLoading = state.isGrantingConsent,
        )
    }
}

/** Sin el consentimiento de IA (CR-2) ninguna función funciona: se ofrece activarlo (coherente con PT2). */
@Composable
private fun ConsentCard(state: AiFeaturesUiState, onActivate: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            AiBadge(label = stringResource(R.string.ai_features_badge))
            Text(
                text = stringResource(R.string.ai_features_consent_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.ai_features_consent_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HealthifyButton(
                text = stringResource(R.string.ai_features_consent_activate),
                onClick = onActivate,
                style = HealthifyButtonStyle.Tonal,
                enabled = state.canChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeaturesCard(state: AiFeaturesUiState, onToggle: (AiFeature, Boolean) -> Unit) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
            AiFeature.entries.forEachIndexed { index, feature ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = dimens.borderThin)
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    HealthifySwitchRow(
                        label = stringResource(feature.titleRes),
                        checked = state.isOn(feature),
                        onCheckedChange = { onToggle(feature, it) },
                        enabled = state.canChange,
                    )
                    Text(
                        text = stringResource(feature.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val AiFeature.titleRes: Int
    get() = when (this) {
        AiFeature.WEEKLY_SUMMARY -> R.string.ai_feature_weekly_summary
        AiFeature.MEAL_IDEAS -> R.string.ai_feature_meal_ideas
        AiFeature.SUGGESTED_QUESTIONS -> R.string.ai_feature_suggested_questions
        AiFeature.MEAL_PHOTO_RECOGNITION -> R.string.ai_feature_meal_photo
    }

private val AiFeature.descriptionRes: Int
    get() = when (this) {
        AiFeature.WEEKLY_SUMMARY -> R.string.ai_feature_weekly_summary_body
        AiFeature.MEAL_IDEAS -> R.string.ai_feature_meal_ideas_body
        AiFeature.SUGGESTED_QUESTIONS -> R.string.ai_feature_suggested_questions_body
        AiFeature.MEAL_PHOTO_RECOGNITION -> R.string.ai_feature_meal_photo_body
    }

private val PreviewAllOn = AiPreferences(
    consentGranted = true,
    weeklySummaryEnabled = true,
    mealIdeasEnabled = true,
    suggestedQuestionsEnabled = true,
    mealPhotoRecognitionEnabled = false,
)

@Preview(name = "PT21.IA · Funciones con IA", widthDp = 360, heightDp = 800)
@Composable
private fun AiFeaturesPreview() {
    HealthifyTheme {
        AiFeaturesContent(state = AiFeaturesUiState(isLoading = false, preferences = PreviewAllOn), actions = AiFeaturesActions(onBack = {}))
    }
}

@Preview(name = "PT21.IA · Sin consentimiento de IA", widthDp = 360, heightDp = 800)
@Composable
private fun AiFeaturesNoConsentPreview() {
    HealthifyTheme {
        AiFeaturesContent(state = AiFeaturesUiState(isLoading = false), actions = AiFeaturesActions(onBack = {}))
    }
}

@Preview(name = "PT21.IA · Activar consentimiento", widthDp = 360, heightDp = 800)
@Composable
private fun AiFeaturesConsentDialogPreview() {
    HealthifyTheme {
        AiFeaturesContent(
            state = AiFeaturesUiState(isLoading = false, consentOffer = ConsentOffer(AiFeature.WEEKLY_SUMMARY)),
            actions = AiFeaturesActions(onBack = {}),
        )
    }
}

@Preview(name = "PT21.IA · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun AiFeaturesOfflinePreview() {
    HealthifyTheme {
        AiFeaturesContent(
            state = AiFeaturesUiState(isLoading = false, isOffline = true, preferences = PreviewAllOn),
            actions = AiFeaturesActions(onBack = {}),
        )
    }
}

@Preview(name = "PT21.IA · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun AiFeaturesLoadingPreview() {
    HealthifyTheme { AiFeaturesContent(state = AiFeaturesUiState(), actions = AiFeaturesActions(onBack = {})) }
}
