package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BiochemistryPanel
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EatingHabits
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EvaluationDetail
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.EvaluationSheetState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReferralDetailItem
import java.time.Instant

/**
 * PAC-3 · detalle de una evaluación del expediente (solo del profesional): medidas, IMC con su categoría, actividad,
 * protocolo, hábitos y bioquímica de esa evaluación.
 *
 * DECISIÓN PAC-3: no hay frame de detalle; se muestra en el BottomSheet del sistema de diseño.
 */
@Composable
fun EvaluationSheet(state: EvaluationSheetState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    HealthifyBottomSheet(onDismissRequest = onDismiss) {
        SheetColumn {
            when (state) {
                EvaluationSheetState.Loading -> repeat(EVALUATION_SKELETON_ROWS) {
                    SkeletonListItem(modifier = Modifier.fillMaxWidth())
                }
                EvaluationSheetState.Failed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
                is EvaluationSheetState.Loaded -> EvaluationDetailBody(state.detail)
            }
        }
    }
}

private const val EVALUATION_SKELETON_ROWS = 4

@Composable
private fun ColumnScope.EvaluationDetailBody(detail: EvaluationDetail) {
    val measurement = detail.measurement
    SheetTitle(
        title = stringResource(R.string.evaluation_detail_title),
        subtitle = listOfNotNull(
            detail.takenAt.mediumDateText(),
            measurement.sex?.let { stringResource(it.summaryRes()) },
            measurement.ageYears.takeIf { it > 0 }
                ?.let { pluralStringResource(R.plurals.baseline_age_years, it, it) },
        ).joinToString(stringResource(R.string.text_separator)),
    )
    DetailGroup {
        DetailRow(stringResource(R.string.measurement_weight), stringResource(R.string.unit_kg_value, decimalText(measurement.weightKg)))
        DetailRow(stringResource(R.string.baseline_height), stringResource(R.string.unit_cm_value, compactText(measurement.heightCm)))
        DetailRow(
            stringResource(R.string.record_pr_bmi),
            listOfNotNull(
                decimalText(measurement.bmi),
                detail.bmiCategory?.let { stringResource(it.labelRes()) },
            ).joinToString(stringResource(R.string.text_separator)),
        )
        DetailRow(
            stringResource(R.string.measurement_waist),
            measurement.waistCm?.let { stringResource(R.string.unit_cm_value, compactText(it)) }
                ?: stringResource(R.string.value_missing),
        )
        DetailRow(
            stringResource(R.string.measurement_body_fat),
            measurement.bodyFatPercentage?.let { compactText(it) + stringResource(R.string.unit_percent) }
                ?: stringResource(R.string.value_missing),
        )
    }
    DetailGroup {
        DetailRow(
            stringResource(R.string.measurement_activity),
            measurement.activityLevel?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.value_missing),
        )
        DetailRow(
            stringResource(R.string.measurement_protocol),
            ProtocolCheck.entries.filter { it in measurement.protocolChecks }
                .map { stringResource(it.labelRes()) }
                .ifEmpty { listOf(stringResource(R.string.value_missing)) }
                .joinToString(stringResource(R.string.list_separator)),
        )
    }
    measurement.habits?.let { HabitsGroup(it) }
    measurement.biochemistry?.let { BiochemistryGroup(it) }
}

@Composable
private fun HabitsGroup(habits: EatingHabits) {
    DetailGroup(title = stringResource(R.string.measurement_habits)) {
        habits.mealsPerDay?.let { DetailRow(stringResource(R.string.measurement_meals_per_day), it.toString()) }
        habits.waterLitersPerDay?.let {
            DetailRow(stringResource(R.string.measurement_water), "${compactText(it)} ${stringResource(R.string.unit_liters_per_day)}")
        }
        habits.mealsOutPerWeek?.let { DetailRow(stringResource(R.string.measurement_meals_out), it.toString()) }
    }
}

@Composable
private fun BiochemistryGroup(panel: BiochemistryPanel) {
    val unit = stringResource(R.string.unit_mg_dl)
    DetailGroup(title = stringResource(R.string.measurement_biochemistry)) {
        panel.glucoseMgDl?.let { DetailRow(stringResource(R.string.measurement_glucose), "${compactText(it)} $unit") }
        panel.totalCholesterolMgDl?.let { DetailRow(stringResource(R.string.measurement_cholesterol), "${compactText(it)} $unit") }
        panel.triglyceridesMgDl?.let { DetailRow(stringResource(R.string.measurement_triglycerides), "${compactText(it)} $unit") }
    }
}

/**
 * Detalle de las derivaciones (expediente del paciente y del nutricionista): especialidad, fecha, estado y motivo. La
 * especialidad y el motivo son texto del profesional; no se traducen.
 */
@Composable
fun ReferralSheet(referrals: List<ReferralDetailItem>, onDismiss: () -> Unit) {
    HealthifyBottomSheet(onDismissRequest = onDismiss) {
        SheetColumn {
            SheetTitle(title = stringResource(R.string.record_referrals_title))
            referrals.forEachIndexed { index, referral ->
                if (index > 0) CardDivider()
                ReferralDetail(referral)
            }
        }
    }
}

@Composable
private fun ReferralDetail(referral: ReferralDetailItem) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.record_pr_referral_title, referral.specialty),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                type = if (referral.isOpen) StatusChipType.PendingConfirmation else StatusChipType.Confirmed,
                label = stringResource(if (referral.isOpen) R.string.referral_detail_open else R.string.referral_detail_closed),
            )
        }
        Text(
            text = listOfNotNull(
                stringResource(R.string.referral_detail_issued_on, referral.issuedAt.mediumDateText()),
                referral.closedAt?.let { stringResource(R.string.referral_detail_closed_on, it.mediumDateText()) },
            ).joinToString(stringResource(R.string.text_separator)),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Text(text = stringResource(R.string.referral_detail_reason), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        Text(
            text = referral.reason.ifBlank { stringResource(R.string.referral_detail_no_reason) },
            style = MaterialTheme.typography.bodyMedium,
            color = if (referral.reason.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
        )
    }
}

@Composable
private fun SheetColumn(content: @Composable ColumnScope.() -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
        content = content,
    )
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        content()
    }
}

/** «Peso ····· 74,2 kg». */
@Composable
private fun DetailRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private val previewEvaluation = EvaluationDetail(
    assessmentId = 2,
    takenAt = Instant.parse("2026-09-03T15:00:00Z"),
    measurement = ConsultationMeasurement(
        weightKg = 74.2,
        heightCm = 168.0,
        waistCm = 88.0,
        bodyFatPercentage = 27.0,
        bmi = 26.3,
        protocolChecks = setOf(ProtocolCheck.entries.first()),
        activityLevel = ActivityLevel.entries.first(),
        habits = EatingHabits(4, 1.5, 2),
        biochemistry = null,
        ageYears = 31,
        sex = BiologicalSex.FEMALE,
    ),
    bmiCategory = DiagnosisCode.OVERWEIGHT_GRADE_I,
)

@Preview(name = "PAC-3 · Detalle de evaluación", widthDp = 360)
@Composable
private fun EvaluationSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame { SheetColumn { EvaluationDetailBody(previewEvaluation) } }
    }
}

@Preview(name = "PAC-3 · Detalle de derivación", widthDp = 360)
@Composable
private fun ReferralSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            SheetColumn {
                ReferralDetail(
                    ReferralDetailItem(
                        specialty = "Endocrinología",
                        issuedAt = Instant.parse("2026-09-08T15:00:00Z"),
                        isOpen = true,
                        reason = "Descartar hipotiroidismo.",
                    ),
                )
            }
        }
    }
}
