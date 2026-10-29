package pe.edu.upc.healthify.features.monitoring.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Minimum width of the Figma date box (52 dp with "SEPT").
private val DateBoxMinWidth = 52.dp

/**
 * The "Next visit" card of My visits, the preparation screen and the sent reply: date, day and time, with whom and
 * how, when it was scheduled, "Add to my calendar" and the reminder to say so if they can't make it. Read-only (the
 * nutritionist schedules it).
 */
@Composable
fun NextConsultationCard(
    next: NextFollowUp,
    onAddToCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val date = next.scheduledFor.atZone(ZoneId.systemDefault())
    HealthifyCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
            Text(
                text = stringResource(R.string.consultations_next_title),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space16), verticalAlignment = Alignment.CenterVertically) {
                val longDate = longDateText(date, locale)
                Column(
                    modifier = Modifier
                        .widthIn(min = DateBoxMinWidth)
                        .clip(MaterialTheme.shapes.large)
                        .background(scheme.primaryContainer)
                        .padding(horizontal = dimens.space12, vertical = dimens.space8)
                        .clearAndSetSemantics { contentDescription = longDate },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = HealthifyTheme.extendedTypography.metricLarge,
                        color = scheme.onPrimaryContainer,
                    )
                    Text(
                        text = shortMonthText(date, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onPrimaryContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    Text(
                        text = stringResource(R.string.consultation_when, weekdayText(date, locale), next.scheduledFor.shortTimeText()),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    val modality = stringResource(next.modality.labelRes)
                    Text(
                        text = next.practitionerFullName
                            ?.let { stringResource(R.string.consultation_with_named_practitioner, it, modality) }
                            ?: stringResource(R.string.consultation_with_practitioner, modality),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    next.scheduledAt?.let { scheduledAt ->
                        Text(
                            text = stringResource(
                                R.string.consultation_scheduled_on,
                                dayMonthText(scheduledAt.atZone(ZoneId.systemDefault()), locale),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HealthifyButton(
                text = stringResource(R.string.consultation_add_to_calendar),
                onClick = onAddToCalendar,
                style = HealthifyButtonStyle.Tonal,
                icon = HealthifyIcons.Calendar,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.consultation_cannot_attend),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * "Add to my calendar": opens the phone's calendar app with the event filled in (the patient decides whether to save
 * it; the app never writes to the calendar). Returns `false` if there is no calendar app.
 *
 * The backend does not report the visit's length, so the event lasts one hour.
 */
fun Context.openAddToCalendar(next: NextFollowUp, title: String, description: String): Boolean {
    val begin = next.scheduledFor.toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, next.scheduledFor.plus(1, ChronoUnit.HOURS).toEpochMilli())
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
