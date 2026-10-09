package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.ReferenceFoodId
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.presentation.components.MealTimeField
import pe.edu.upc.healthify.features.intake.presentation.components.MealTimePickerDialog
import pe.edu.upc.healthify.features.intake.presentation.components.PlanAdherenceCard
import pe.edu.upc.healthify.features.intake.presentation.components.numberText
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealEvent
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealUiState
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.ManualMealViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

// «Card · seleccionado» de PT10.1: marca ✓ de 22 dp. «Estado vacío · No lo encontramos»: círculo de 64 dp, lupa de 28.
private val SelectedMarkSize = 22.dp
private val SelectedMarkIcon = 14.dp
private val NotFoundCircle = 64.dp
private val NotFoundIcon = 28.dp

data class ManualMealActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSearchAgain: () -> Unit = {},
    val onFoodSelected: (ReferenceFood) -> Unit = {},
    val onPortionChange: (String) -> Unit = {},
    val onPlanAnswer: (Boolean) -> Unit = {},
    val onMealTimeClick: () -> Unit = {},
    val onMealTimeSelected: (LocalDateTime) -> Unit = {},
    val onMealTimeDismiss: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** PT9 / PT9.K / PT10 / PT10.1 / PT10.3 · Registrar a mano (o elegir un alimento para PT8). */
@Composable
fun ManualMealScreen(
    onBack: () -> Unit,
    onLogged: (MealLogOutcome) -> Unit,
    onFoodPicked: (referenceFoodId: Long, name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualMealViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is ManualMealEvent.Logged -> onLogged(event.outcome)
            is ManualMealEvent.FoodPicked -> onFoodPicked(event.referenceFoodId, event.name)
        }
    }
    ManualMealContent(
        state = state,
        actions = ManualMealActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onSearchAgain = viewModel::onSearchAgain,
            onFoodSelected = viewModel::onFoodSelected,
            onPortionChange = viewModel::onPortionChange,
            onPlanAnswer = viewModel::onPlanAnswer,
            onMealTimeClick = viewModel::onMealTimeClick,
            onMealTimeSelected = viewModel::onMealTimeSelected,
            onMealTimeDismiss = viewModel::onMealTimeDismiss,
            onSubmit = viewModel::onSubmit,
            onRetry = viewModel::onRetry,
        ),
        modifier = modifier,
    )
}

@Composable
fun ManualMealContent(state: ManualMealUiState, actions: ManualMealActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    if (state.saveFailed) {
        // PT10.3 · pantalla de error sin barra superior (como el frame).
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
                title = stringResource(R.string.meal_photo_save_failed_title),
                text = stringResource(R.string.manual_save_failed_body),
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
        HealthifyTopAppBar(
            title = stringResource(if (state.pickFoodOnly) R.string.manual_pick_title else R.string.manual_title),
            onBack = actions.onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            HealthifyTextField(
                value = state.query,
                onValueChange = actions.onQueryChange,
                label = stringResource(R.string.manual_search_label),
                placeholder = stringResource(R.string.manual_search_placeholder),
                supportingText = if (state.localOnly) {
                    stringResource(R.string.manual_offline_note)
                } else {
                    stringResource(R.string.manual_search_support)
                },
                errorText = when (state.foodError) {
                    ManualMealUiState.FoodError.NOT_CHOSEN -> stringResource(R.string.manual_choose_food)
                    ManualMealUiState.FoodError.NOT_RESOLVED -> stringResource(R.string.food_not_resolved)
                    null -> null
                },
                imeAction = ImeAction.Search,
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon = { Icon(HealthifyIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                state.isSearching -> Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                    repeat(SKELETON_ROWS) { SkeletonListItem() }
                }
                state.notFound -> NotFound(localOnly = state.localOnly, onSearchAgain = actions.onSearchAgain)
                state.results.isNotEmpty() -> Results(state = state, onSelect = actions.onFoodSelected)
            }
            if (!state.notFound) {
                if (state.pickFoodOnly) {
                    HealthifyButton(
                        text = stringResource(R.string.manual_pick_food),
                        onClick = actions.onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    MealFields(state = state, actions = actions)
                }
            }
        }
    }
    if (state.form.showTimePicker) {
        MealTimePickerDialog(
            initial = state.form.mealTime.toLocalDateTime(),
            today = state.today,
            earliestDay = state.earliestDay,
            onConfirm = actions.onMealTimeSelected,
            onDismiss = actions.onMealTimeDismiss,
        )
    }
}

@Composable
private fun Results(state: ManualMealUiState, onSelect: (ReferenceFood) -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        val term = state.searchedTerm
        if (term != null) {
            Text(
                text = pluralStringResource(R.plurals.manual_results_count, state.results.size, state.results.size, term),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.results.forEach { food ->
            FoodResultCard(food = food, selected = food.id == state.selectedFood?.id, onClick = { onSelect(food) })
        }
    }
}

/** «Card · Arroz blanco cocido» (y «Card · seleccionado» en verde con ✓). */
@Composable
private fun FoodResultCard(food: ReferenceFood, selected: Boolean, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val selectedDescription = stringResource(R.string.manual_cd_selected)
    val kcal = stringResource(R.string.manual_food_kcal, numberText(food.energyKcalPer100g))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest)
            .then(if (selected) Modifier.border(dimens.borderThick, scheme.primary, shape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(dimens.space16)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                if (selected) contentDescription = "${food.name}, $kcal, $selectedDescription"
            },
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) scheme.primary else scheme.onSurface,
            )
            Text(text = kcal, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(SelectedMarkSize)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Icon(HealthifyIcons.Check, contentDescription = null, tint = scheme.surfaceContainerLowest, modifier = Modifier.size(SelectedMarkIcon))
            }
        }
    }
}

/** PT10 · «No lo encontramos»: limitación del catálogo, nunca un error del paciente. */
@Composable
private fun NotFound(localOnly: Boolean, onSearchAgain: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(NotFoundCircle)
                .clip(CircleShape)
                .background(scheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(HealthifyIcons.Search, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(NotFoundIcon))
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space4), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.manual_not_found_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.manual_not_found_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (localOnly) {
                Text(
                    text = stringResource(R.string.manual_offline_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        HealthifyButton(
            text = stringResource(R.string.manual_search_again),
            onClick = onSearchAgain,
            style = HealthifyButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** «Porción (g)», «¿Cuándo comiste?», «¿Estaba en tu plan?» (con un alimento elegido) y «Registrar». */
@Composable
private fun MealFields(state: ManualMealUiState, actions: ManualMealActions) {
    val dimens = HealthifyTheme.dimens
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
        HealthifyTextField(
            value = form.portionText,
            onValueChange = actions.onPortionChange,
            label = stringResource(R.string.adjust_portion_label),
            placeholder = stringResource(R.string.portion_placeholder),
            errorText = if (form.showPortionError) stringResource(R.string.portion_error) else null,
            type = HealthifyTextFieldType.Decimal,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth(),
        )
        MealTimeField(
            value = form.mealTime,
            today = state.today,
            onClick = actions.onMealTimeClick,
            errorText = mealTimeErrorText(form.mealTimeError),
        )
    }
    if (state.selectedFood != null) {
        PlanAdherenceCard(answer = form.inPlan, onAnswer = actions.onPlanAnswer, showError = form.showPlanError)
    }
    HealthifyButton(
        text = stringResource(R.string.manual_register),
        onClick = actions.onSubmit,
        loading = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun mealTimeErrorText(error: LocalTimestamp.Validity?): String? = when (error) {
    LocalTimestamp.Validity.TOO_OLD -> stringResource(R.string.meal_time_too_old)
    LocalTimestamp.Validity.IN_FUTURE -> stringResource(R.string.meal_time_future)
    LocalTimestamp.Validity.VALID, null -> null
}

private const val SKELETON_ROWS = 3

private val PreviewForm = MealFormState(mealTime = OffsetDateTime.parse("2026-09-09T13:15:00-05:00"))
private val PreviewToday = LocalDate.parse("2026-09-09")
private val PreviewFoods = listOf(
    ReferenceFood(ReferenceFoodId(1), "Quinua cocida", 120.0, 4.4, 21.3, 1.9),
    ReferenceFood(ReferenceFoodId(2), "Ensalada de quinua", 140.0, 4.0, 18.0, 5.0),
    ReferenceFood(ReferenceFoodId(3), "Quinua con leche", 95.0, 3.0, 16.0, 2.0),
)

@Preview(name = "PT9 · Buscar alimento", widthDp = 360, heightDp = 800)
@Composable
private fun ManualMealPreview() {
    HealthifyTheme { ManualMealContent(ManualMealUiState(today = PreviewToday, form = PreviewForm), ManualMealActions()) }
}

@Preview(name = "PT10.1 · Alimento encontrado", widthDp = 360, heightDp = 800)
@Composable
private fun ManualMealFoundPreview() {
    HealthifyTheme {
        ManualMealContent(
            ManualMealUiState(
                today = PreviewToday,
                form = PreviewForm.copy(portionText = "150"),
                query = "quinua",
                results = PreviewFoods,
                searchedTerm = "quinua",
                selectedFood = PreviewFoods.first(),
            ),
            ManualMealActions(),
        )
    }
}

@Preview(name = "PT10 · No encontrado (sin conexión)", widthDp = 360, heightDp = 800)
@Composable
private fun ManualMealNotFoundPreview() {
    HealthifyTheme {
        ManualMealContent(
            ManualMealUiState(today = PreviewToday, form = PreviewForm, query = "chaufa de cuy", searchedTerm = "chaufa de cuy", localOnly = true),
            ManualMealActions(),
        )
    }
}

@Preview(name = "PT9 · buscando", widthDp = 360, heightDp = 800)
@Composable
private fun ManualMealSearchingPreview() {
    HealthifyTheme {
        ManualMealContent(ManualMealUiState(today = PreviewToday, form = PreviewForm, query = "qui", isSearching = true), ManualMealActions())
    }
}

@Preview(name = "PT10.3 · No se pudo registrar", widthDp = 360, heightDp = 800)
@Composable
private fun ManualMealFailedPreview() {
    HealthifyTheme { ManualMealContent(ManualMealUiState(today = PreviewToday, form = PreviewForm, saveFailed = true), ManualMealActions()) }
}
