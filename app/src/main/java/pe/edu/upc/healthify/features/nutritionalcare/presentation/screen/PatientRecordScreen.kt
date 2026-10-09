package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTag
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ListCardStyle
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MyNumbers
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordClinicalWeight
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordCompliance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordNextFollowUp
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordPlan
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordPractitioner
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordReferral
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.toUiText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientRecordUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PatientRecordViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Avatar del nutricionista del Figma.
private val AvatarSize = 48.dp

/**
 * PT20 · Mi expediente (+ PT20.L), pestaña «Expediente» del shell. Solo lo propio del paciente: nunca diagnóstico,
 * base de cálculo, IMC ni su categoría (RM-4, regla ética 1).
 */
@Composable
fun PatientRecordScreen(
    onOpenMyConsultations: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientRecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PatientRecordContent(
        state = state,
        onRetry = viewModel::onRetry,
        onOpenMyConsultations = onOpenMyConsultations,
        modifier = modifier,
    )
}

@Composable
fun PatientRecordContent(
    state: PatientRecordUiState,
    onRetry: () -> Unit,
    onOpenMyConsultations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.record_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            val record = state.record
            when {
                record != null -> RecordBody(record, onOpenMyConsultations)
                state.isLoading -> repeat(4) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
                state.loadFailed && state.isOffline -> EmptyState(
                    title = stringResource(R.string.record_offline_title),
                    text = stringResource(R.string.record_offline_body),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ColumnScope.RecordBody(record: PatientOwnRecord, onOpenMyConsultations: () -> Unit) {
    PractitionerCard(record.practitioner)
    ConsultationsCard(record.nextFollowUp, onOpenMyConsultations)
    MyNumbersSection(record.myNumbers)
    PlanSection(record.plan, record.openReferrals)
}

/** «Card · Mi nutricionista». Sin datos (el contexto no respondió): la sección queda vacía. */
@Composable
private fun PractitionerCard(practitioner: RecordPractitioner?) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    RecordCard {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = HealthifyIcons.Person, contentDescription = null, tint = scheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                // DECISIÓN PT20: el frame rotula «Tu nutricionista»; con el nombre del read model se muestra el nombre.
                Text(
                    text = practitioner?.fullName ?: stringResource(R.string.record_practitioner_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = practitioner?.linkedSince
                        ?.let { stringResource(R.string.record_practitioner_since, it.mediumDateText()) }
                        ?: stringResource(R.string.record_section_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (practitioner?.isActive == true) {
                StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.record_practitioner_active))
            }
        }
    }
}

/** «Card · Mis consultas» → PT25 (también sin próxima consulta: muestra las anteriores). */
@Composable
private fun ConsultationsCard(next: RecordNextFollowUp?, onOpen: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    RecordCard(onClick = onOpen) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = HealthifyIcons.Calendar, contentDescription = null, tint = scheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(
                    text = stringResource(R.string.record_consultations_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
                Text(
                    text = next?.let {
                        stringResource(R.string.record_consultations_next, weekdayDayMonth(it.scheduledFor), it.scheduledFor.shortTimeText())
                    } ?: stringResource(R.string.record_consultations_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** «MIS NÚMEROS»: cada número que falta se muestra como «—»; sin ninguno, la sección vacía de PT20.L. */
@Composable
private fun MyNumbersSection(numbers: MyNumbers) {
    val dimens = HealthifyTheme.dimens
    val locale = currentLocale()
    Section(title = stringResource(R.string.record_numbers_title)) {
        if (numbers.isEmpty) {
            EmptySectionCard(
                title = stringResource(R.string.record_numbers_empty_title),
                text = stringResource(R.string.record_numbers_empty_body),
            )
            return@Section
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            NumberCard(
                label = stringResource(R.string.record_numbers_energy),
                value = numbers.energyTargetKcal?.let {
                    stringResource(R.string.record_numbers_energy_value, NumberFormat.getIntegerInstance(locale).format(it))
                },
                caption = numbers.planVersion
                    ?.let { stringResource(R.string.record_numbers_energy_caption, it) }
                    ?: stringResource(R.string.record_numbers_per_day),
                modifier = Modifier.weight(1f),
            )
            ComplianceCard(numbers.compliance, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            ClinicalWeightCard(numbers.clinicalWeight, locale, Modifier.weight(1f))
            NumberCard(
                label = stringResource(R.string.record_numbers_trend),
                value = numbers.weightSlopeKgPerWeek?.let { slopeText(it, locale) },
                caption = stringResource(R.string.record_numbers_trend_caption),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Un día sin registro no es incumplimiento (regla ética 3): solo se cuentan los días cumplidos, sin rojo. */
@Composable
private fun ComplianceCard(compliance: RecordCompliance?, modifier: Modifier) {
    NumberCard(
        label = stringResource(R.string.record_numbers_compliance),
        value = compliance?.let { pluralStringResource(R.plurals.record_numbers_compliance_value, it.total, it.met, it.total) },
        caption = (compliance?.rangeDays ?: COMPLIANCE_DAYS).let {
            pluralStringResource(R.plurals.record_numbers_compliance_caption, it, it)
        },
        captionColorPrimary = true,
        modifier = modifier,
    )
}

/** Medición de la consulta (nunca un autopesaje, regla ética 6). */
@Composable
private fun ClinicalWeightCard(weight: RecordClinicalWeight?, locale: Locale, modifier: Modifier) {
    NumberCard(
        label = stringResource(R.string.record_numbers_clinical_weight),
        value = weight?.let { stringResource(R.string.record_numbers_kg_value, UiText.formatNumber(it.kg, locale)) },
        caption = weight
            ?.let { stringResource(R.string.record_numbers_clinical_weight_caption, shortDayMonth(it.takenAt, locale)) }
            ?: stringResource(R.string.record_numbers_clinical_weight_none),
        modifier = modifier,
    )
}

@Composable
private fun NumberCard(
    label: String,
    value: String?,
    caption: String,
    modifier: Modifier = Modifier,
    captionColorPrimary: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = modifier, shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Text(
                text = value ?: stringResource(R.string.record_numbers_missing),
                style = HealthifyTheme.extendedTypography.metricMedium,
                color = scheme.onSurface,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = if (captionColorPrimary && value != null) scheme.primary else scheme.onSurfaceVariant,
            )
        }
    }
}

/** «MI PLAN» (indicaciones y restricciones traducidas; lo escrito por el nutricionista, tal cual) y «Derivaciones». */
@Composable
private fun PlanSection(plan: RecordPlan?, openReferrals: List<RecordReferral>) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Section(title = stringResource(R.string.record_plan_title)) {
        if (plan == null) {
            EmptySectionCard(
                title = stringResource(R.string.record_plan_empty_title),
                text = stringResource(R.string.record_plan_empty_body),
            )
        } else {
            RecordCard(verticalSpacing = dimens.space16) {
                val guidelines = plan.guidelines.mapNotNull { it.toPlanGuideline() }.map { it.toUiText().asString() }
                val restrictions = (
                    plan.restrictions.mapNotNull(DietaryRestriction::ofCode) +
                        plan.legacyRestrictions.mapNotNull(DietaryRestriction::legacy)
                    ).map { it.toUiText().asString() }
                TagGroup(stringResource(R.string.record_plan_guidelines), guidelines, stringResource(R.string.record_plan_no_guidelines))
                TagGroup(stringResource(R.string.record_plan_restrictions), restrictions, stringResource(R.string.record_plan_no_restrictions))
            }
        }
        RecordCard {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = HealthifyIcons.Forward, contentDescription = null, tint = scheme.onSurfaceVariant)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    Text(
                        text = stringResource(R.string.record_referrals_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = if (openReferrals.isEmpty()) {
                            stringResource(R.string.record_referrals_none)
                        } else {
                            openReferrals.joinToString(separator = " · ") { it.specialty }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun RecordGuideline.toPlanGuideline(): PlanGuideline? = PlanGuideline.of(code, custom)

@Composable
private fun TagGroup(title: String, tags: List<String>, emptyText: String) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        if (tags.isEmpty()) {
            Text(text = emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                tags.forEach { HealthifyTag(text = it) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = title,
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun RecordCard(
    onClick: (() -> Unit)? = null,
    verticalSpacing: Dp = HealthifyTheme.dimens.space12,
    content: @Composable ColumnScope.() -> Unit,
) {
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, onClick = onClick) {
        Column(
            modifier = Modifier.padding(HealthifyTheme.dimens.space16),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

/** «Card · Mis medidas» de PT20.L: una sección vacía no tumba el resto. */
@Composable
private fun EmptySectionCard(title: String, text: String) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = Modifier.fillMaxWidth(), style = ListCardStyle.Outlined) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** «jueves 18 sept.» / «Thursday, Sep 18». */
@Composable
private fun weekdayDayMonth(instant: Instant): String {
    val locale = currentLocale()
    val pattern = if (locale.language == Locale.ENGLISH.language) "EEEE, MMM d" else "EEEE d MMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(instant.atZone(ZoneId.systemDefault()))
}

/** «3 mar.» / «Mar 3». */
private fun shortDayMonth(instant: Instant, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "MMM d" else "d MMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(instant.atZone(ZoneId.systemDefault()))
}

/** «−0,3 kg/sem» / «+0.2 kg/wk»: con signo tipográfico. */
@Composable
private fun slopeText(kgPerWeek: Double, locale: Locale): String {
    val sign = when {
        kgPerWeek < 0 -> "−"
        kgPerWeek > 0 -> "+"
        else -> ""
    }
    return stringResource(R.string.record_numbers_trend_value, sign + UiText.formatNumber(abs(kgPerWeek), locale))
}

private const val COMPLIANCE_DAYS = 7

private val PreviewRecord = PatientOwnRecord(
    practitioner = RecordPractitioner("Lucía Ramos", Instant.parse("2026-03-12T15:00:00Z"), isActive = true),
    nextFollowUp = RecordNextFollowUp(31, Instant.parse("2026-09-18T15:00:00Z")),
    myNumbers = MyNumbers(
        energyTargetKcal = 1850,
        planVersion = 3,
        compliance = RecordCompliance(LocalDate.parse("2026-09-09"), LocalDate.parse("2026-09-15"), met = 5, total = 7),
        clinicalWeight = RecordClinicalWeight(74.2, Instant.parse("2026-03-03T15:00:00Z")),
        weightSlopeKgPerWeek = -0.3,
    ),
    plan = RecordPlan(
        guidelines = listOf(
            RecordGuideline("PrioritizeVegetables", null),
            RecordGuideline("AvoidSugaryDrinks", null),
            RecordGuideline("ReduceSalt", null),
        ),
        restrictions = listOf("ShellfishFree"),
        legacyRestrictions = emptyList(),
    ),
    referrals = emptyList(),
)

@Preview(name = "PT20 · Mi expediente", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRecordPreview() {
    HealthifyTheme {
        PatientRecordContent(state = PatientRecordUiState(isLoading = false, record = PreviewRecord), onRetry = {}, onOpenMyConsultations = {})
    }
}

@Preview(name = "PT20.L · Sección vacía", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRecordEmptySectionPreview() {
    HealthifyTheme {
        PatientRecordContent(
            state = PatientRecordUiState(
                isLoading = false,
                record = PreviewRecord.copy(practitioner = null, myNumbers = MyNumbers.Empty, plan = null),
            ),
            onRetry = {},
            onOpenMyConsultations = {},
        )
    }
}

@Preview(name = "PT20.L · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRecordLoadingPreview() {
    HealthifyTheme { PatientRecordContent(state = PatientRecordUiState(), onRetry = {}, onOpenMyConsultations = {}) }
}

@Preview(name = "PT20 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRecordOfflinePreview() {
    HealthifyTheme {
        PatientRecordContent(
            state = PatientRecordUiState(isLoading = false, isOffline = true, loadFailed = true),
            onRetry = {},
            onOpenMyConsultations = {},
        )
    }
}

@Preview(name = "PT20 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun PatientRecordErrorPreview() {
    HealthifyTheme {
        PatientRecordContent(state = PatientRecordUiState(isLoading = false, loadFailed = true), onRetry = {}, onOpenMyConsultations = {})
    }
}
