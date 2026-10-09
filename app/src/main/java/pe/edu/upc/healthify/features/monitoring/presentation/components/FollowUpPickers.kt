package pe.edu.upc.healthify.features.monitoring.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogProperties
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogScrim
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val DialogMaxWidth = 312.dp

/** Fecha de PR17 (Material 3): desde hoy (la hora decide si el momento es futuro). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDatePickerDialog(
    initial: LocalDate?,
    today: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis
            override fun isSelectableYear(year: Int): Boolean = year >= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            HealthifyButton(
                text = stringResource(R.string.common_accept),
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
                style = HealthifyButtonStyle.Text,
            )
        },
        dismissButton = {
            HealthifyButton(text = stringResource(R.string.ds_cancel), onClick = onDismiss, style = HealthifyButtonStyle.Text)
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** Hora de PR17. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpTimePickerDialog(initial: LocalTime?, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val title = stringResource(R.string.schedule_time_dialog_title)
    val start = initial ?: LocalTime.of(DEFAULT_HOUR, 0)
    val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute)
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
                HealthifyButton(text = stringResource(R.string.ds_cancel), onClick = onDismiss, style = HealthifyButtonStyle.Text)
                HealthifyButton(
                    text = stringResource(R.string.common_accept),
                    onClick = { onConfirm(LocalTime.of(timeState.hour, timeState.minute)) },
                    style = HealthifyButtonStyle.Text,
                )
            }
        }
    }
}

private const val DEFAULT_HOUR = 9
