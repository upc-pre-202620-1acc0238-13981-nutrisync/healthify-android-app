package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogProperties
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogScrim
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.ReadOnlyClickableField
import pe.edu.upc.healthify.core.designsystem.component.SegmentedSelector
import pe.edu.upc.healthify.core.designsystem.component.YesNoSelector
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

// Ancho máximo del Dialog del Figma (312 dp).
private val DialogMaxWidth = 312.dp

/** Card «¿Estaba en tu plan?» de PT7, PT8 y PT10.1 (IN-1): SelectorSiNo sin valor por defecto + aclaración. */
@Composable
fun PlanAdherenceCard(
    answer: Boolean?,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showError: Boolean = false,
    enabled: Boolean = true,
) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            YesNoSelector(
                question = stringResource(R.string.plan_adherence_question),
                answer = answer,
                onAnswer = onAnswer,
                isError = showError,
                enabled = enabled,
            )
            Text(
                text = stringResource(R.string.plan_adherence_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Campo «¿Cuándo comiste?» (Filled): muestra «Hoy · 1:15 p. m.» y abre el selector al tocarlo. Solo lectura si
 * [enabled] es `false` (una entrada ya registrada: su momento no se reescribe).
 *
 * @param errorText «Este registro es de hace más de 48 h…» o «Elige un momento que ya pasó.».
 */
@Composable
fun MealTimeField(
    value: OffsetDateTime,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    enabled: Boolean = true,
    supportingText: String = stringResource(R.string.meal_time_support),
    label: String = stringResource(R.string.meal_time_label),
) {
    ReadOnlyClickableField(
        value = mealTimeText(value, today),
        label = label,
        onClick = onClick,
        modifier = modifier,
        supportingText = supportingText,
        errorText = errorText,
        enabled = enabled,
        trailingIcon = HealthifyIcons.Clock,
    )
}


/**
 * Selector de «¿Cuándo comiste?» (y «¿Cuándo te pesaste?» de PT12): día (Hoy / Ayer / Anteayer, dentro de la ventana
 * de 48 h) y hora. Devuelve la fecha y hora del teléfono; el ViewModel le pone el offset y valida la regla de 48 h con
 * `LocalTimestamp`.
 *
 * @param earliestDay día en que se creó la cuenta: no se ofrecen días anteriores (con solo «Hoy» no hay selector).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTimePickerDialog(
    initial: LocalDateTime,
    today: LocalDate,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.meal_time_dialog_title),
    earliestDay: LocalDate? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val days = pickableDays(today, earliestDay)
    var dayIndex by rememberSaveable { mutableIntStateOf(days.indexOf(initial.toLocalDate()).coerceAtLeast(0)) }
    val timeState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    Dialog(onDismissRequest = onDismiss, properties = HealthifyDialogProperties) {
        HealthifyDialogScrim()
        val shape = RoundedCornerShape(dimens.radiusDialog)
        Column(
            modifier = Modifier
                .padding(horizontal = dimens.space24)
                .widthIn(max = DialogMaxWidth)
                .fillMaxWidth()
                .elevation(dimens.elevation3, shape)
                .clip(shape)
                .background(scheme.surfaceContainerLowest)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space24, top = dimens.space24, end = dimens.space24, bottom = dimens.space16)
                .semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(dimens.space16),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (days.size > 1) {
                SegmentedSelector(
                    options = days.indices.toList(),
                    selected = dayIndex,
                    onSelect = { dayIndex = it },
                    optionLabel = { index ->
                        stringResource(
                            when (index) {
                                0 -> R.string.meal_time_day_today
                                1 -> R.string.meal_time_day_yesterday
                                else -> R.string.meal_time_day_before_yesterday
                            },
                        )
                    },
                )
            }
            TimePicker(
                state = timeState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = scheme.surfaceContainer,
                    selectorColor = scheme.primary,
                    timeSelectorSelectedContainerColor = scheme.primaryContainer,
                    timeSelectorSelectedContentColor = scheme.onPrimaryContainer,
                    timeSelectorUnselectedContainerColor = scheme.surfaceContainer,
                    periodSelectorSelectedContainerColor = scheme.primaryContainer,
                    periodSelectorSelectedContentColor = scheme.onPrimaryContainer,
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.space8, Alignment.End),
            ) {
                HealthifyButton(
                    text = stringResource(R.string.ds_cancel),
                    onClick = onDismiss,
                    style = HealthifyButtonStyle.Text,
                )
                HealthifyButton(
                    text = stringResource(R.string.meal_time_done),
                    onClick = {
                        onConfirm(LocalDateTime.of(days[dayIndex], LocalTime.of(timeState.hour, timeState.minute)))
                    },
                    style = HealthifyButtonStyle.Text,
                )
            }
        }
    }
}

/** Hoy, ayer y anteayer (ventana de 48 h), sin los días anteriores a la cuenta. */
internal fun pickableDays(today: LocalDate, earliestDay: LocalDate?): List<LocalDate> =
    listOf(today, today.minusDays(1), today.minusDays(2))
        .filter { earliestDay == null || !it.isBefore(minOf(earliestDay, today)) }

@Preview(name = "Partes del formulario · plan y hora", widthDp = 360, heightDp = 520)
@Composable
private fun MealFormPartsPreview() {
    HealthifyTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(HealthifyTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16),
        ) {
            AiBadge(label = "Ideas con IA")
            PlanAdherenceCard(answer = null, onAnswer = {})
            MealTimeField(
                value = OffsetDateTime.parse("2026-09-09T13:15:00-05:00"),
                today = LocalDate.parse("2026-09-09"),
                onClick = {},
            )
        }
    }
}
