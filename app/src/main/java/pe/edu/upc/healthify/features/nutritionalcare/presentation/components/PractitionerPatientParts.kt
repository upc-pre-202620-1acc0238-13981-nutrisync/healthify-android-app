package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.InitialAvatar
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ComplianceRatio
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * «Card · Paciente» de la ficha: inicial, nombre, «Plan versión 3 · en tu cartera desde…» y el chip «Vigente» o
 * «Nuevo ingreso» (sin datos base o sin plan).
 */
@Composable
fun PatientHeaderCard(
    fullName: String?,
    linkedSince: Instant?,
    activePlanVersion: Int?,
    isNew: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val name = fullName ?: stringResource(R.string.patient_unnamed)
    val since = linkedSince?.let { stringResource(R.string.patient_in_roster_since, it.mediumDateText()) }
    val subtitle = when {
        activePlanVersion != null -> listOfNotNull(stringResource(R.string.patient_plan_version, activePlanVersion), since)
        else -> listOfNotNull(since, stringResource(R.string.patient_without_plan))
    }.joinToString(stringResource(R.string.text_separator)).replaceFirstChar { it.titlecase(currentLocale()) }
    SectionCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(initial = name.take(1).uppercase(currentLocale()))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            when {
                isNew -> StatusChip(
                    type = StatusChipType.PendingConfirmation,
                    label = stringResource(R.string.patient_new_chip),
                )
                activePlanVersion != null -> StatusChip(
                    type = StatusChipType.Confirmed,
                    label = stringResource(R.string.patient_plan_active_chip),
                )
            }
        }
    }
}

/** «5 de 7 días». Un día sin registro no cuenta como incumplimiento: solo no suma. */
@Composable
fun complianceText(ratio: ComplianceRatio): String =
    pluralStringResource(R.plurals.days_of_total, ratio.total, ratio.met, ratio.total)

/** «−0,3 kg/sem» (con el signo menos tipográfico). */
@Composable
fun slopeText(kgPerWeek: Double): String {
    val sign = when {
        kgPerWeek < 0 -> stringResource(R.string.sign_minus)
        kgPerWeek > 0 -> stringResource(R.string.sign_plus)
        else -> ""
    }
    return stringResource(R.string.unit_kg_per_week, sign + decimalText(abs(kgPerWeek)))
}

/** «se guardó hoy» / «se guardó ayer» / «se guardó el 3 sept.». */
@Composable
fun savedWhenText(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = at.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> stringResource(R.string.saved_today)
        today.minusDays(1) -> stringResource(R.string.saved_yesterday)
        else -> stringResource(R.string.saved_on, shortDateText(date))
    }
}

/** «3 sept.» en el idioma de la app. */
@Composable
fun shortDateText(date: LocalDate): String =
    DateTimeFormatter.ofPattern(stringResource(R.string.pattern_day_month), currentLocale()).format(date)
