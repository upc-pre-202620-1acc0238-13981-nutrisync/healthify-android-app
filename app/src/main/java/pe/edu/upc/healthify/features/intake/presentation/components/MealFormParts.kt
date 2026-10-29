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
import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.entity.PlanFitReason
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

// Max width of the design's Dialog (312 dp).
private val DialogMaxWidth = 312.dp

/**
 * The "Was it in your plan?" card: yes/no selector + a note. Without [suggestion] it has no default value;
 * with it, it arrives preselected, says why and warns if the dish seems to break a plan restriction.
 * The patient can always change it.
 */
@Composable
fun PlanAdherenceCard(
    answer: Boolean?,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showError: Boolean = false,
    enabled: Boolean = true,
    suggestion: PlanFit? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            if (suggestion != null) AiBadge(label = stringResource(R.string.plan_suggestion_badge))
            YesNoSelector(
                question = stringResource(R.string.plan_adherence_question),
                answer = answer,
                onAnswer = onAnswer,
                isError = showError,
                enabled = enabled,
            )
            if (suggestion != null) {
                Text(
                    text = planSuggestionText(suggestion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                if (suggestion.breaksRestrictions) RestrictionWarning(suggestion)
                if (answer != null && answer != suggestion.suggestsInPlan) {
                    Text(
                        text = stringResource(R.string.plan_suggestion_changed),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.plan_adherence_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** "We suggest Yes: it seems to fit your plan." and its variants. */
@Composable
private fun planSuggestionText(suggestion: PlanFit): String {
    val remaining = suggestion.remainingKcal
    return when (suggestion.reason) {
        PlanFitReason.FITS -> stringResource(R.string.plan_suggestion_fits)
        PlanFitReason.BREAKS_RESTRICTION -> stringResource(R.string.plan_suggestion_breaks_restriction)
        PlanFitReason.EXCEEDS_REMAINING -> if (remaining != null) {
            stringResource(R.string.plan_suggestion_exceeds, numberText(suggestion.energyKcal), numberText(remaining))
        } else {
            stringResource(R.string.plan_suggestion_exceeds_no_day)
        }
    }
}

/** Restriction warning: it invites a review, never accuses. */
@Composable
private fun RestrictionWarning(suggestion: PlanFit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val names = suggestion.violatedRestrictions.map { restrictionName(it) }.joinToString()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.radiusMd))
            .background(scheme.errorContainer)
            .padding(dimens.space12)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(HealthifyIcons.Warning, contentDescription = null, tint = scheme.onErrorContainer)
        Text(
            text = stringResource(R.string.plan_restriction_warning, names),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onErrorContainer,
        )
    }
}

/**
 * The "When did you eat?" field (Filled): shows "Today · 1:15 p.m." and opens the picker on tap. Read-only if
 * [enabled] is `false` (an entry already logged: its moment is not rewritten).
 *
 * @param errorText "This log is more than 48 h old…" or "Pick a moment that has already passed.".
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
 * Picker for "When did you eat?" (and "When did you weigh yourself?"): day (Today / Yesterday / Day before, within the
 * 48 h window) and time. Returns the phone's date and time; the ViewModel adds the offset and checks the 48 h rule with
 * `LocalTimestamp`.
 *
 * @param earliestDay the day the account was created: no earlier days are offered (only "Today" means no picker).
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

/** Today, yesterday and the day before (48 h window), without days before the account existed. */
internal fun pickableDays(today: LocalDate, earliestDay: LocalDate?): List<LocalDate> =
    listOf(today, today.minusDays(1), today.minusDays(2))
        .filter { earliestDay == null || !it.isBefore(minOf(earliestDay, today)) }

@Preview(name = "Form parts - plan and time", widthDp = 360, heightDp = 520)
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
