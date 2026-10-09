package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.ReadOnlyClickableField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * PT8 · «Card (contenedor) · Ajustar porción» + «¿Estaba en tu plan?» + «Confirmar». Lo usan el flujo por foto y
 * «Confirmar porción» de una entrada del diario. Ningún juicio sobre la porción: la propuesta y lo confirmado se
 * guardan juntos.
 *
 * @param proposedGrams lo que propuso la IA («Lo que propusimos: 320 g. Ambos valores quedan guardados.»).
 * @param timeLocked el momento ya se registró y no se reescribe (entrada existente).
 */
@Composable
fun AdjustPortionForm(
    foodName: String,
    form: MealFormState,
    today: LocalDate,
    proposedGrams: Double?,
    onChangeFood: () -> Unit,
    onPortionChange: (String) -> Unit,
    onMealTimeClick: () -> Unit,
    onPlanAnswer: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    foodNotResolved: Boolean = false,
    timeLocked: Boolean = false,
    confirming: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.space24)) {
        HealthifyCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
                ReadOnlyClickableField(
                    value = foodName,
                    label = stringResource(R.string.adjust_food_label),
                    onClick = onChangeFood,
                    supportingText = stringResource(R.string.adjust_food_support),
                    errorText = if (foodNotResolved) stringResource(R.string.food_not_resolved) else null,
                    trailingIcon = HealthifyIcons.Edit,
                )
                HealthifyTextField(
                    value = form.portionText,
                    onValueChange = onPortionChange,
                    label = stringResource(R.string.adjust_portion_label),
                    placeholder = stringResource(R.string.portion_placeholder),
                    supportingText = proposedGrams?.let { stringResource(R.string.adjust_portion_proposed, numberText(it)) },
                    errorText = if (form.showPortionError) stringResource(R.string.portion_error) else null,
                    type = HealthifyTextFieldType.Decimal,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                MealTimeField(
                    value = form.mealTime,
                    today = today,
                    onClick = onMealTimeClick,
                    enabled = !timeLocked,
                    supportingText = stringResource(if (timeLocked) R.string.adjust_time_locked else R.string.meal_time_support),
                    errorText = when (form.mealTimeError) {
                        LocalTimestamp.Validity.TOO_OLD -> stringResource(R.string.meal_time_too_old)
                        LocalTimestamp.Validity.IN_FUTURE -> stringResource(R.string.meal_time_future)
                        LocalTimestamp.Validity.VALID, null -> null
                    },
                )
            }
        }
        PlanAdherenceCard(answer = form.inPlan, onAnswer = onPlanAnswer, showError = form.showPlanError)
        HealthifyButton(
            text = stringResource(R.string.adjust_confirm),
            onClick = onConfirm,
            loading = confirming,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "PT8 · Ajustar porción", widthDp = 360, heightDp = 800)
@Composable
private fun AdjustPortionFormPreview() {
    HealthifyTheme {
        AdjustPortionForm(
            foodName = "Lomo saltado",
            form = MealFormState(portionText = "300", mealTime = OffsetDateTime.parse("2026-09-09T13:10:00-05:00")),
            today = LocalDate.parse("2026-09-09"),
            proposedGrams = 320.0,
            onChangeFood = {},
            onPortionChange = {},
            onMealTimeClick = {},
            onPlanAnswer = {},
            onConfirm = {},
            modifier = Modifier.padding(HealthifyTheme.dimens.space16),
        )
    }
}
