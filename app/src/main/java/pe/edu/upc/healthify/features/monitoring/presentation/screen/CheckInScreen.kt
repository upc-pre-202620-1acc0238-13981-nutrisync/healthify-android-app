package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SegmentedSelector
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.presentation.components.labelRes
import pe.edu.upc.healthify.features.monitoring.presentation.state.CheckInUiState
import pe.edu.upc.healthify.features.monitoring.presentation.state.OwnQuestionError
import pe.edu.upc.healthify.features.monitoring.presentation.state.SuggestionOption
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.CheckInEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.CheckInViewModel

data class CheckInActions(
    val onBack: () -> Unit,
    val onRetryLoad: () -> Unit = {},
    val onFeelingSelected: (PlanFeeling) -> Unit = {},
    val onDifficultyToggled: (CheckInDifficulty, Boolean) -> Unit = { _, _ -> },
    val onOwnQuestionChanged: (String) -> Unit = {},
    val onSuggestionToggled: (String, Boolean) -> Unit = { _, _ -> },
    val onSubmit: () -> Unit = {},
    val onRetrySubmit: () -> Unit = {},
    val onDismissSubmitError: () -> Unit = {},
)

/** PT25.2 · Cuéntale cómo te fue (+ PT25.2.E). Al enviar vuelve a PT25 (PT25.3). */
@Composable
fun CheckInScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            CheckInEvent.Submitted -> onSubmitted()
        }
    }
    CheckInContent(
        state = state,
        actions = CheckInActions(
            onBack = onBack,
            onRetryLoad = viewModel::onRetryLoad,
            onFeelingSelected = viewModel::onFeelingSelected,
            onDifficultyToggled = viewModel::onDifficultyToggled,
            onOwnQuestionChanged = viewModel::onOwnQuestionChanged,
            onSuggestionToggled = viewModel::onSuggestionToggled,
            onSubmit = viewModel::onSubmit,
            onRetrySubmit = viewModel::onRetrySubmit,
            onDismissSubmitError = viewModel::onDismissSubmitError,
        ),
        modifier = modifier,
    )
}

@Composable
fun CheckInContent(
    state: CheckInUiState,
    actions: CheckInActions,
    modifier: Modifier = Modifier,
) {
    if (state.submitFailed) {
        SubmitErrorContent(state, actions, modifier)
        return
    }
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.check_in_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space8, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            when {
                state.isLoading -> repeat(3) { SkeletonCard(modifier = Modifier.fillMaxWidth()) }
                state.loadFailed && state.isOffline -> EmptyState(
                    title = stringResource(R.string.consultations_offline_title),
                    text = stringResource(R.string.consultations_offline_body),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadFailed -> ErrorState(onRetry = actions.onRetryLoad, modifier = Modifier.fillMaxWidth())
                else -> CheckInForm(state, actions)
            }
        }
        if (!state.isLoading && !state.loadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(dimens.space16),
            ) {
                HealthifyButton(
                    text = stringResource(if (state.isSubmitting) R.string.check_in_sending else R.string.check_in_send),
                    onClick = actions.onSubmit,
                    enabled = state.canSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    when {
        state.isLocked -> HealthifyDialog(
            title = stringResource(R.string.check_in_locked_title),
            text = stringResource(R.string.check_in_locked_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onBack,
            onDismiss = actions.onBack,
            dismissLabel = null,
        )
        state.notScheduled -> HealthifyDialog(
            title = stringResource(R.string.check_in_not_scheduled_title),
            text = stringResource(R.string.check_in_not_scheduled_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onBack,
            onDismiss = actions.onBack,
            dismissLabel = null,
        )
    }
}

@Composable
private fun ColumnScope.CheckInForm(state: CheckInUiState, actions: CheckInActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val enabled = !state.isSubmitting && !state.isLocked
    FormCard {
        CardTitle(stringResource(R.string.check_in_feeling_question))
        SegmentedSelector(
            options = PlanFeeling.entries,
            selected = state.feeling,
            onSelect = actions.onFeelingSelected,
            optionLabel = { stringResource(it.labelRes) },
            enabled = enabled,
            isError = state.feelingMissing,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.feelingMissing) {
            Text(
                text = stringResource(R.string.check_in_feeling_required),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
            )
        }
    }
    FormCard {
        CardTitle(stringResource(R.string.check_in_difficulties_question))
        Text(
            text = stringResource(R.string.check_in_difficulties_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            CheckInDifficulty.entries.forEach { difficulty ->
                HealthifyFilterChip(
                    text = stringResource(difficulty.labelRes),
                    selected = difficulty in state.difficulties,
                    onSelectedChange = { actions.onDifficultyToggled(difficulty, it) },
                    enabled = enabled,
                )
            }
        }
    }
    FormCard {
        HealthifyTextField(
            value = state.ownQuestion,
            onValueChange = actions.onOwnQuestionChanged,
            label = stringResource(R.string.check_in_question_label),
            placeholder = stringResource(R.string.check_in_question_placeholder),
            supportingText = stringResource(
                R.string.check_in_question_counter,
                state.ownQuestion.length,
                CheckInUiState.MAX_QUESTION_LENGTH,
            ),
            errorText = when (state.ownQuestionError) {
                OwnQuestionError.TOO_SHORT -> stringResource(R.string.check_in_question_too_short)
                OwnQuestionError.TOO_LONG -> stringResource(R.string.check_in_question_too_long)
                null -> null
            },
            singleLine = false,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (state.suggestions.isNotEmpty()) SuggestionsCard(state.suggestions, state.canSelectMoreSuggestions, enabled, actions)
}

/** «También podrías preguntar»: las sugerencias de IA van tal cual (en el idioma en que se generaron). */
@Composable
private fun SuggestionsCard(
    suggestions: List<SuggestionOption>,
    canSelectMore: Boolean,
    enabled: Boolean,
    actions: CheckInActions,
) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            AiBadge(label = stringResource(R.string.ai_suggestion_badge))
            CardTitle(stringResource(R.string.check_in_suggestions_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
                suggestions.forEach { suggestion ->
                    HealthifyFilterChip(
                        text = suggestion.text,
                        selected = suggestion.selected,
                        onSelectedChange = { actions.onSuggestionToggled(suggestion.text, it) },
                        enabled = enabled && (suggestion.selected || canSelectMore),
                    )
                }
            }
            if (!canSelectMore) {
                Text(
                    text = pluralStringResource(
                        R.plurals.check_in_suggestions_limit,
                        CheckInUiState.MAX_AI_QUESTIONS,
                        CheckInUiState.MAX_AI_QUESTIONS,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
            content = content,
        )
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

/** PT25.2.E · No se pudo enviar: lo escrito se conserva; «atrás» vuelve al formulario. */
@Composable
private fun SubmitErrorContent(state: CheckInUiState, actions: CheckInActions, modifier: Modifier) {
    val dimens = HealthifyTheme.dimens
    BackHandler(onBack = actions.onDismissSubmitError)
    SystemBarsAppearance(darkBackground = false)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.space16, vertical = dimens.space40),
        contentAlignment = Alignment.Center,
    ) {
        FailureState(
            title = stringResource(R.string.check_in_error_title),
            text = stringResource(R.string.check_in_error_body),
            actionLabel = stringResource(R.string.check_in_error_retry),
            onAction = actions.onRetrySubmit,
            actionLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val PreviewForm = CheckInUiState(
    isLoading = false,
    feeling = PlanFeeling.FAIR,
    difficulties = setOf(CheckInDifficulty.DINNERS, CheckInDifficulty.WEEKENDS),
    ownQuestion = "¿Puedo comer fuera los viernes?",
    suggestions = listOf(
        SuggestionOption("¿Cómo armo cenas con más proteína?", selected = true, aiGenerationId = 7, language = "es"),
        SuggestionOption("¿Puedo ajustar el plan los fines de semana?", selected = false, aiGenerationId = 7, language = "es"),
    ),
)

@Preview(name = "PT25.2 · Cuéntale cómo te fue", widthDp = 360, heightDp = 800)
@Composable
private fun CheckInPreview() {
    HealthifyTheme { CheckInContent(state = PreviewForm, actions = CheckInActions(onBack = {})) }
}

@Preview(name = "PT25.2 · Falta cómo te sentiste", widthDp = 360, heightDp = 800)
@Composable
private fun CheckInMissingFeelingPreview() {
    HealthifyTheme {
        CheckInContent(
            state = CheckInUiState(isLoading = false, feelingMissing = true, ownQuestionError = OwnQuestionError.TOO_SHORT, ownQuestion = "¿y"),
            actions = CheckInActions(onBack = {}),
        )
    }
}

@Preview(name = "PT25.2.E · No se pudo enviar", widthDp = 360, heightDp = 800)
@Composable
private fun CheckInErrorPreview() {
    HealthifyTheme { CheckInContent(state = PreviewForm.copy(submitFailed = true), actions = CheckInActions(onBack = {})) }
}

@Preview(name = "PT25.2 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun CheckInLoadingPreview() {
    HealthifyTheme { CheckInContent(state = CheckInUiState(), actions = CheckInActions(onBack = {})) }
}

@Preview(name = "PT25.2 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun CheckInOfflinePreview() {
    HealthifyTheme {
        CheckInContent(state = PreviewForm.copy(isOffline = true), actions = CheckInActions(onBack = {}))
    }
}
