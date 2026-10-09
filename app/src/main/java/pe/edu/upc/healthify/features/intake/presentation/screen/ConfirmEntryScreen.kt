package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.presentation.components.AdjustPortionForm
import pe.edu.upc.healthify.features.intake.presentation.state.ConfirmEntryEvent
import pe.edu.upc.healthify.features.intake.presentation.state.ConfirmEntryUiState
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.ConfirmEntryViewModel
import java.time.LocalDate
import java.time.OffsetDateTime

data class ConfirmEntryActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onChangeFood: () -> Unit = {},
    val onPortionChange: (String) -> Unit = {},
    val onPlanAnswer: (Boolean) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onAlreadyConfirmedAcknowledged: () -> Unit = {},
)

/** PT8 · Confirmar o ajustar una entrada «Por confirmar» del diario. */
@Composable
fun ConfirmEntryScreen(
    onBack: () -> Unit,
    onPickFood: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ConfirmEntryEvent.PickFood -> onPickFood()
            ConfirmEntryEvent.Done -> onDone()
        }
    }
    ConfirmEntryContent(
        state = state,
        actions = ConfirmEntryActions(
            onBack = onBack,
            onRetryLoad = viewModel::onRetryLoad,
            onChangeFood = viewModel::onChangeFood,
            onPortionChange = viewModel::onPortionChange,
            onPlanAnswer = viewModel::onPlanAnswer,
            onConfirm = viewModel::onConfirm,
            onDismissError = viewModel::onDismissDialogs,
            onAlreadyConfirmedAcknowledged = viewModel::onAlreadyConfirmedAcknowledged,
        ),
        modifier = modifier,
    )
}

@Composable
fun ConfirmEntryContent(state: ConfirmEntryUiState, actions: ConfirmEntryActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.adjust_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            val food = state.food
            when {
                state.isLoading -> SkeletonCard()
                state.loadFailed || food == null -> if (!state.showAlreadyConfirmed) {
                    ErrorState(onRetry = actions.onRetryLoad, modifier = Modifier.fillMaxWidth())
                }
                else -> AdjustPortionForm(
                    foodName = food.name,
                    form = state.form,
                    today = state.today,
                    proposedGrams = state.proposedGrams,
                    onChangeFood = actions.onChangeFood,
                    onPortionChange = actions.onPortionChange,
                    onMealTimeClick = {},
                    onPlanAnswer = actions.onPlanAnswer,
                    onConfirm = actions.onConfirm,
                    foodNotResolved = state.foodNotResolved,
                    timeLocked = true,
                    confirming = state.isSaving,
                )
            }
        }
    }
    when {
        state.showAlreadyConfirmed -> HealthifyDialog(
            title = stringResource(R.string.adjust_title),
            text = stringResource(R.string.adjust_already_confirmed),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onAlreadyConfirmedAcknowledged,
            onDismiss = actions.onAlreadyConfirmedAcknowledged,
            dismissLabel = null,
        )
        state.showError -> ServerErrorDialog(onRetry = actions.onConfirm, onDismiss = actions.onDismissError)
    }
}

@Preview(name = "PT8 · Confirmar porción (entrada del diario)", widthDp = 360, heightDp = 800)
@Composable
private fun ConfirmEntryPreview() {
    val food = MealFood(12, "Ceviche")
    HealthifyTheme {
        ConfirmEntryContent(
            state = ConfirmEntryUiState(
                today = LocalDate.parse("2026-09-09"),
                form = MealFormState(portionText = "280", mealTime = OffsetDateTime.parse("2026-09-09T14:10:00-05:00")),
                isLoading = false,
                proposedFood = food,
                proposedGrams = 280.0,
                food = food,
            ),
            actions = ConfirmEntryActions(),
        )
    }
}

@Preview(name = "PT8 · cargando", widthDp = 360, heightDp = 800)
@Composable
private fun ConfirmEntryLoadingPreview() {
    HealthifyTheme {
        ConfirmEntryContent(
            state = ConfirmEntryUiState(
                today = LocalDate.parse("2026-09-09"),
                form = MealFormState(mealTime = OffsetDateTime.parse("2026-09-09T14:10:00-05:00")),
            ),
            actions = ConfirmEntryActions(),
        )
    }
}
