package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.YesNoSelector
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.presentation.components.MealTimeField
import pe.edu.upc.healthify.features.intake.presentation.components.MealTimePickerDialog
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInEvent
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInUiState
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInUiState.WeightError
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.SelfWeighInViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class SelfWeighInActions(
    val onBack: () -> Unit = {},
    val onWeightChange: (String) -> Unit = {},
    val onFastedAnswer: (Boolean) -> Unit = {},
    val onTimeClick: () -> Unit = {},
    val onTimeSelected: (LocalDateTime) -> Unit = {},
    val onTimeDismiss: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** PT12 · Autopesaje (+ PT12.E, PT12.2). Se abre desde PT13 «Registrar autopesaje». */
@Composable
fun SelfWeighInScreen(
    onBack: () -> Unit,
    onSaved: (SelfWeighInOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelfWeighInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is SelfWeighInEvent.Saved -> onSaved(event.outcome)
        }
    }
    SelfWeighInContent(
        state = state,
        actions = SelfWeighInActions(
            onBack = onBack,
            onWeightChange = viewModel::onWeightChange,
            onFastedAnswer = viewModel::onFastedAnswer,
            onTimeClick = viewModel::onTimeClick,
            onTimeSelected = viewModel::onTimeSelected,
            onTimeDismiss = viewModel::onTimeDismiss,
            onSave = viewModel::onSave,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

@Composable
fun SelfWeighInContent(state: SelfWeighInUiState, actions: SelfWeighInActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    if (state.saveFailed) {
        // PT12.2 · pantalla de error sin barra superior (como el frame); lo escrito no se pierde.
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.space16, vertical = dimens.space40),
            contentAlignment = Alignment.Center,
        ) {
            FailureState(
                title = stringResource(R.string.self_weigh_in_failed_title),
                text = stringResource(R.string.self_weigh_in_failed_body),
                actionLabel = stringResource(R.string.meal_photo_retry),
                onAction = actions.onRetry,
                actionLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.self_weigh_in_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
            HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                HealthifyTextField(
                    value = state.weightText,
                    onValueChange = actions.onWeightChange,
                    label = stringResource(R.string.self_weigh_in_weight_label),
                    placeholder = stringResource(R.string.self_weigh_in_weight_placeholder),
                    errorText = when (state.weightError) {
                        WeightError.EMPTY -> stringResource(R.string.self_weigh_in_weight_empty)
                        WeightError.OUT_OF_RANGE -> stringResource(R.string.self_weigh_in_weight_out_of_range)
                        null -> null
                    },
                    // PT12.E: teclado decimal.
                    type = HealthifyTextFieldType.Decimal,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.space16),
                )
            }
            MealTimeField(
                value = state.weighedAt,
                today = state.today,
                onClick = actions.onTimeClick,
                label = stringResource(R.string.self_weigh_in_time_label),
                errorText = when (state.timeError) {
                    LocalTimestamp.Validity.TOO_OLD -> stringResource(R.string.self_weigh_in_time_too_old)
                    LocalTimestamp.Validity.IN_FUTURE -> stringResource(R.string.meal_time_future)
                    LocalTimestamp.Validity.VALID, null -> null
                },
            )
            HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space8),
                ) {
                    Text(
                        text = stringResource(R.string.self_weigh_in_protocol),
                        style = HealthifyTheme.extendedTypography.overlineSection,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    YesNoSelector(
                        question = stringResource(R.string.self_weigh_in_fasted_question),
                        answer = state.fasted,
                        onAnswer = actions.onFastedAnswer,
                        isError = state.showFastedError,
                        errorText = stringResource(R.string.self_weigh_in_fasted_required),
                    )
                }
            }
        }
        // «Acciones»: botón fijo (sube con el teclado, como PT12.E).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(dimens.space16),
        ) {
            HealthifyButton(
                text = stringResource(R.string.self_weigh_in_save),
                onClick = actions.onSave,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.showTimePicker) {
        MealTimePickerDialog(
            initial = state.weighedAt.toLocalDateTime(),
            today = state.today,
            earliestDay = state.earliestDay,
            onConfirm = actions.onTimeSelected,
            onDismiss = actions.onTimeDismiss,
            title = stringResource(R.string.self_weigh_in_time_dialog_title),
        )
    }
}

private val PreviewState = SelfWeighInUiState(
    today = LocalDate.parse("2026-09-09"),
    weighedAt = OffsetDateTime.parse("2026-09-09T07:30:00-05:00"),
)

@Preview(name = "PT12 · Autopesaje", widthDp = 360, heightDp = 800)
@Composable
private fun SelfWeighInPreview() {
    HealthifyTheme { SelfWeighInContent(PreviewState, SelfWeighInActions()) }
}

@Preview(name = "PT12.E · Fuera de rango", widthDp = 360, heightDp = 800)
@Composable
private fun SelfWeighInOutOfRangePreview() {
    HealthifyTheme {
        SelfWeighInContent(
            PreviewState.copy(weightText = "684", weightError = WeightError.OUT_OF_RANGE, showFastedError = true),
            SelfWeighInActions(),
        )
    }
}

@Preview(name = "PT12 · Guardando sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun SelfWeighInOfflinePreview() {
    HealthifyTheme {
        SelfWeighInContent(
            PreviewState.copy(weightText = "68,4", fasted = true, isOffline = true, isSaving = true),
            SelfWeighInActions(),
        )
    }
}

@Preview(name = "PT12.2 · Error al guardar", widthDp = 360, heightDp = 800)
@Composable
private fun SelfWeighInFailedPreview() {
    HealthifyTheme { SelfWeighInContent(PreviewState.copy(saveFailed = true), SelfWeighInActions()) }
}
