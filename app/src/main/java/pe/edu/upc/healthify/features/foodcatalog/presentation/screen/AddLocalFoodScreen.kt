package pe.edu.upc.healthify.features.foodcatalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.LocalFoodField
import pe.edu.upc.healthify.features.foodcatalog.presentation.state.AddLocalFoodUiState
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.AddLocalFoodEvent
import pe.edu.upc.healthify.features.foodcatalog.presentation.viewmodel.AddLocalFoodViewModel

data class AddLocalFoodActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onEnergyChange: (String) -> Unit = {},
    val onProteinChange: (String) -> Unit = {},
    val onCarbChange: (String) -> Unit = {},
    val onFatChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onServerErrorDismissed: () -> Unit = {},
)

/** PR15.1 · Agregar alimento local. */
@Composable
fun AddLocalFoodScreen(
    onBack: () -> Unit,
    onAdded: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddLocalFoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is AddLocalFoodEvent.Added -> onAdded(event.name)
        }
    }
    AddLocalFoodContent(
        state = state,
        actions = AddLocalFoodActions(
            onBack = onBack,
            onNameChange = viewModel::onNameChange,
            onEnergyChange = viewModel::onEnergyChange,
            onProteinChange = viewModel::onProteinChange,
            onCarbChange = viewModel::onCarbChange,
            onFatChange = viewModel::onFatChange,
            onSave = viewModel::onSave,
            onServerErrorDismissed = viewModel::onServerErrorDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun AddLocalFoodContent(state: AddLocalFoodUiState, actions: AddLocalFoodActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val enabled = !state.isSaving
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.local_food_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            Text(text = stringResource(R.string.local_food_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            HealthifyTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = stringResource(R.string.local_food_name),
                placeholder = stringResource(R.string.local_food_name_placeholder),
                supportingText = stringResource(R.string.local_food_name_support),
                errorText = when {
                    state.duplicatedName -> stringResource(R.string.local_food_duplicated)
                    LocalFoodField.NAME in state.invalidFields -> stringResource(R.string.local_food_incomplete)
                    else -> null
                },
                enabled = enabled,
                type = HealthifyTextFieldType.Name,
                imeAction = ImeAction.Next,
            )
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                SectionOverline(text = stringResource(R.string.local_food_values))
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.space16)) {
                    NutrientField(state.energy, actions.onEnergyChange, R.string.local_food_energy, R.string.local_food_energy_example,
                        LocalFoodField.ENERGY in state.invalidFields, enabled, Modifier.weight(1f))
                    NutrientField(state.protein, actions.onProteinChange, R.string.local_food_protein, R.string.local_food_protein_example,
                        LocalFoodField.PROTEIN in state.invalidFields, enabled, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.space16)) {
                    NutrientField(state.carb, actions.onCarbChange, R.string.local_food_carb, R.string.local_food_carb_example,
                        LocalFoodField.CARB in state.invalidFields, enabled, Modifier.weight(1f))
                    NutrientField(state.fat, actions.onFatChange, R.string.local_food_fat, R.string.local_food_fat_example,
                        LocalFoodField.FAT in state.invalidFields, enabled, Modifier.weight(1f))
                }
                val nutrientInvalid = state.invalidFields.any { it != LocalFoodField.NAME }
                if (nutrientInvalid) {
                    Text(
                        text = stringResource(R.string.local_food_incomplete),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                }
            }
        }
        HealthifyButton(
            text = stringResource(if (state.isSaving) R.string.common_saving else R.string.local_food_save),
            onClick = actions.onSave,
            loading = state.isSaving,
            enabled = !state.isOffline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
        )
    }
    if (state.showServerError) ServerErrorDialog(onRetry = actions.onSave, onDismiss = actions.onServerErrorDismissed)
}

@Composable
private fun NutrientField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    placeholderRes: Int,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    HealthifyTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(labelRes),
        placeholder = stringResource(placeholderRes),
        isError = isError,
        enabled = enabled,
        type = HealthifyTextFieldType.Decimal,
        imeAction = ImeAction.Next,
        modifier = modifier,
    )
}

@Preview(name = "PR15.1 · Agregar alimento local", widthDp = 360, heightDp = 800)
@Composable
private fun AddLocalFoodPreview() {
    HealthifyTheme { AddLocalFoodContent(state = AddLocalFoodUiState(), actions = AddLocalFoodActions()) }
}

@Preview(name = "PR15.1 · Faltan valores", widthDp = 360, heightDp = 800)
@Composable
private fun AddLocalFoodInvalidPreview() {
    HealthifyTheme {
        AddLocalFoodContent(
            state = AddLocalFoodUiState(name = "Chaufa de cuy", invalidFields = setOf(LocalFoodField.ENERGY, LocalFoodField.FAT)),
            actions = AddLocalFoodActions(),
        )
    }
}

@Preview(name = "PR15.1 · Nombre duplicado", widthDp = 360, heightDp = 800)
@Composable
private fun AddLocalFoodDuplicatedPreview() {
    HealthifyTheme {
        AddLocalFoodContent(
            state = AddLocalFoodUiState(name = "Cuy al horno", duplicatedName = true),
            actions = AddLocalFoodActions(),
        )
    }
}

@Preview(name = "PR15.1 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun AddLocalFoodOfflinePreview() {
    HealthifyTheme { AddLocalFoodContent(state = AddLocalFoodUiState(isOffline = true), actions = AddLocalFoodActions()) }
}
