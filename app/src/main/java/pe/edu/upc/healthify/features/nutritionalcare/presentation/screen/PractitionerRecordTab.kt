package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.IconInfoRow
import pe.edu.upc.healthify.core.designsystem.component.MetricCard
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ClinicalWeightSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ComplianceRatio
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordActiveTargets
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordBmi
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordComplianceWindow
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordDiagnosis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordEvaluation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReferralSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.compactText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.complianceText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.decimalText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.shortDateText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PractitionerRecordUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PractitionerRecordViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * PAC-3 · pestaña Expediente: peso clínico, IMC, metas vigentes, cumplimiento, **diagnóstico activo** (solo del
 * profesional), derivaciones y evaluaciones.
 *
 * DECISIÓN PAC-3: el frame dibuja chevrons en diagnóstico, derivación y evaluaciones, pero no hay pantallas de
 * detalle; las filas se muestran sin chevron y no son táctiles. Cerrar una derivación queda para PR16.
 */
@Composable
fun PractitionerRecordTab(
    modifier: Modifier = Modifier,
    viewModel: PractitionerRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PractitionerRecordContent(state = state, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun PractitionerRecordContent(
    state: PractitionerRecordUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PatientTabLayout(modifier = modifier) {
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        val record = state.record
        when {
            record != null -> RecordBody(record)
            state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
            state.loadFailed && state.isOffline -> NeedsConnection(onRetry)
            state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val SKELETON_ROWS = 4

@Composable
private fun ColumnScope.RecordBody(record: PractitionerPatientRecord) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        IndicatorRow {
            ClinicalWeightCard(record.clinicalWeight, Modifier.weight(1f).fillMaxHeight())
            BmiCard(record.bmi, Modifier.weight(1f).fillMaxHeight())
        }
        IndicatorRow {
            TargetsCard(record.activeTargets, Modifier.weight(1f).fillMaxHeight())
            ComplianceCard(record.compliance, Modifier.weight(1f).fillMaxHeight())
        }
        DiagnosisCard(record.activeDiagnosis)
        record.referrals.forEach { ReferralCard(it) }
    }
    EvaluationsSection(record.evaluations)
}

@Composable
private fun IndicatorRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        content = content,
    )
}

/** «74.2 kg · −1.8 kg desde marzo». */
@Composable
private fun ClinicalWeightCard(weight: ClinicalWeightSummary?, modifier: Modifier) {
    MetricCard(
        label = stringResource(R.string.record_pr_clinical_weight),
        value = weight?.let { stringResource(R.string.unit_kg_value, decimalText(it.latestKg)) }
            ?: stringResource(R.string.value_missing),
        caption = weight?.let {
            val delta = it.deltaKgSinceFirst
            val sign = when {
                delta < 0 -> stringResource(R.string.sign_minus)
                delta > 0 -> stringResource(R.string.sign_plus)
                else -> ""
            }
            stringResource(R.string.record_pr_weight_delta, sign + decimalText(abs(delta)), monthText(it.firstDate))
        } ?: stringResource(R.string.record_pr_no_measurement),
        captionColor = if (weight != null) MaterialTheme.colorScheme.primary else null,
        modifier = modifier,
    )
}

@Composable
private fun BmiCard(bmi: RecordBmi?, modifier: Modifier) {
    MetricCard(
        label = stringResource(R.string.record_pr_bmi),
        value = bmi?.let { decimalText(it.value) } ?: stringResource(R.string.value_missing),
        caption = bmi?.category?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.record_pr_no_measurement),
        modifier = modifier,
    )
}

@Composable
private fun TargetsCard(targets: RecordActiveTargets?, modifier: Modifier) {
    MetricCard(
        label = stringResource(R.string.record_pr_active_targets),
        value = targets?.let { stringResource(R.string.unit_kcal_value, compactText(it.energyKcal, 0)) }
            ?: stringResource(R.string.value_missing),
        caption = targets?.let {
            stringResource(R.string.record_pr_version_on, it.planVersion, shortDateText(it.validFrom.localDate()))
        } ?: stringResource(R.string.record_pr_no_plan),
        modifier = modifier,
    )
}

@Composable
private fun ComplianceCard(compliance: RecordComplianceWindow?, modifier: Modifier) {
    MetricCard(
        label = stringResource(R.string.summary_compliance_label),
        value = compliance?.let { complianceText(it.ratio) } ?: stringResource(R.string.value_missing),
        caption = stringResource(R.string.summary_compliance_last_week),
        modifier = modifier,
    )
}

/** «Diagnóstico activo · Sobrepeso grado I · 3 mar. 2026 · solo profesional». El paciente nunca lo ve. */
@Composable
private fun DiagnosisCard(diagnosis: RecordDiagnosis?) {
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Lock,
            title = stringResource(R.string.record_pr_active_diagnosis),
            supportingText = diagnosis?.let {
                val label = it.code?.let { code -> stringResource(code.labelRes()) } ?: it.statement
                stringResource(R.string.record_pr_diagnosis_line, label, it.issuedAt.mediumDateText())
            } ?: stringResource(R.string.record_pr_no_diagnosis),
        )
    }
}

/** «Derivación a Endocrinología · 8 sept. 2026 · en curso». La especialidad es texto del profesional. */
@Composable
private fun ReferralCard(referral: ReferralSummary) {
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Forward,
            title = stringResource(R.string.record_pr_referral_title, referral.specialty),
            supportingText = stringResource(
                R.string.record_pr_referral_line,
                referral.issuedAt.mediumDateText(),
                stringResource(if (referral.isOpen) R.string.record_pr_referral_open else R.string.record_pr_referral_closed),
            ),
        )
    }
}

/** «EVALUACIONES»: «74.2 kg · IMC 26.3 · cintura 88 cm» / «… · primera evaluación». */
@Composable
private fun EvaluationsSection(evaluations: List<RecordEvaluation>) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        SectionOverline(stringResource(R.string.record_pr_evaluations))
        SectionCard {
            if (evaluations.isEmpty()) {
                IconInfoRow(icon = HealthifyIcons.Evaluation, title = stringResource(R.string.record_pr_no_evaluations))
            }
            evaluations.forEachIndexed { index, evaluation ->
                if (index > 0) CardDivider()
                val parts = listOfNotNull(
                    stringResource(R.string.unit_kg_value, decimalText(evaluation.weightKg)),
                    stringResource(R.string.record_pr_bmi_value, decimalText(evaluation.bmi)),
                    when {
                        evaluation.isFirst -> stringResource(R.string.record_pr_first_evaluation)
                        evaluation.waistCm != null ->
                            stringResource(R.string.record_pr_waist_value, compactText(evaluation.waistCm))
                        else -> null
                    },
                )
                IconInfoRow(
                    icon = HealthifyIcons.Evaluation,
                    title = evaluation.takenAt.mediumDateText(),
                    supportingText = parts.joinToString(stringResource(R.string.text_separator)),
                )
            }
        }
    }
}

private fun Instant.localDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

/** «marzo» (el mes de la primera medición). */
@Composable
private fun monthText(at: Instant): String =
    DateTimeFormatter.ofPattern(stringResource(R.string.pattern_month), pe.edu.upc.healthify.core.designsystem.component.currentLocale())
        .format(at.localDate())

private val previewRecord = PractitionerPatientRecord(
    clinicalWeight = ClinicalWeightSummary(
        74.2,
        Instant.parse("2026-09-03T15:00:00Z"),
        -1.8,
        Instant.parse("2026-03-03T15:00:00Z"),
    ),
    bmi = RecordBmi(26.3, DiagnosisCode.OVERWEIGHT_GRADE_I),
    activeTargets = RecordActiveTargets(3, Instant.parse("2026-09-04T15:00:00Z"), 1796.0),
    compliance = RecordComplianceWindow(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-06"), ComplianceRatio(5, 7)),
    activeDiagnosis = RecordDiagnosis(DiagnosisCode.OVERWEIGHT_GRADE_I, "", Instant.parse("2026-03-03T15:00:00Z")),
    referrals = listOf(ReferralSummary(1, "Endocrinología", Instant.parse("2026-09-08T15:00:00Z"), isOpen = true)),
    evaluations = listOf(
        RecordEvaluation(2, Instant.parse("2026-09-03T15:00:00Z"), 74.2, 26.3, 88.0, isFirst = false),
        RecordEvaluation(1, Instant.parse("2026-03-03T15:00:00Z"), 76.0, 26.9, 90.0, isFirst = true),
    ),
)

@Preview(name = "PAC-3 · Expediente", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerRecordPreview() {
    HealthifyTheme {
        PractitionerRecordContent(state = PractitionerRecordUiState(isLoading = false, record = previewRecord), onRetry = {})
    }
}

@Preview(name = "PAC-3 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerRecordLoadingPreview() {
    HealthifyTheme { PractitionerRecordContent(state = PractitionerRecordUiState(), onRetry = {}) }
}

@Preview(name = "PAC-3 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerRecordOfflinePreview() {
    HealthifyTheme {
        PractitionerRecordContent(
            state = PractitionerRecordUiState(isLoading = false, isOffline = true, loadFailed = true),
            onRetry = {},
        )
    }
}
