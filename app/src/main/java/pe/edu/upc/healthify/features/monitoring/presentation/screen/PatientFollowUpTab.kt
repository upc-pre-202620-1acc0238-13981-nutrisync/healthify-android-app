package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.StatusChip
import pe.edu.upc.healthify.core.designsystem.component.StatusChipType
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.DaysRatio
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelClinicalMeasurement
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelDiaryEntry
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelEntryProvenance
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelProtocolCheck
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeek
import pe.edu.upc.healthify.features.monitoring.domain.entity.PanelWeightTrend
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientFollowUpUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.PatientFollowUpViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.abs

// "This week" chart (compact dashboard): bars with 4 dp radius; height by the day's outcome (not a quantity).
private val BarMaxHeight = 52.dp
private val BarMetHeight = 42.dp
private val BarShortHeight = 24.dp
private val BarUnloggedHeight = 8.dp
private val LegendDotSize = 8.dp
private const val CHEVRON_DOWN = 90f
private const val CHEVRON_UP = 270f

/**
 * Follow-up tab (compact dashboard, so a small phone needs little scrolling): the Mon–Sun week with its full legend, three
 * indicators in one row (self-weighing trend, days with logs, last clinical measurement), and two accordions for the
 * AI-generated summary ("Review it before using it in a visit") and today's **read-only** diary. Both accordions start
 * open; nothing is hidden, only collapsible.
 *
 * "Above" uses `tertiary` and "No log" is a low outlined mark with no fill: a day without logs is not non-compliance and
 * is never drawn as "Below" or in red. The legend always lists the four outcomes, even those the week does not have, so
 * the colors can be read without remembering them.
 */
@Composable
fun PatientFollowUpTab(
    modifier: Modifier = Modifier,
    viewModel: PatientFollowUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PatientFollowUpContent(state = state, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun PatientFollowUpContent(state: PatientFollowUpUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) {
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        val panel = state.panel
        when {
            panel != null -> FollowUpBody(panel, state.aiSummary)
            state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
            state.loadFailed && state.isOffline -> EmptyState(
                title = stringResource(R.string.roster_offline_title),
                text = stringResource(R.string.patient_offline_body),
                actionLabel = stringResource(R.string.ds_retry),
                onAction = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
            state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

private const val SKELETON_ROWS = 4

@Composable
private fun ColumnScope.FollowUpBody(panel: PatientMonitoringPanel, aiSummary: String?) {
    panel.week?.let { WeekCard(it) }
    IndicatorsRow(panel)
    if (!aiSummary.isNullOrBlank()) AiSummaryAccordion(aiSummary)
    DiaryAccordion(panel.diary)
    Text(
        text = stringResource(R.string.follow_up_read_only),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekCard(week: PanelWeek) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(verticalSpacing = dimens.space8) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.follow_up_this_week),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.days_of_total, week.totalDays, week.metDays, week.totalDays),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalAlignment = Alignment.Bottom,
        ) {
            week.days.forEach { day -> DayBar(day, Modifier.weight(1f)) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            ComplianceOutcome.entries.forEach { outcome -> LegendItem(outcome) }
        }
    }
}

@Composable
private fun DayBar(day: PanelDay, modifier: Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val letter = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
    val description = stringResource(
        R.string.follow_up_day_cd,
        day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
        stringResource(day.outcome.legendRes()),
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.space4),
    ) {
        Box(modifier = Modifier.height(BarMaxHeight).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            val shape = MaterialTheme.shapes.extraSmall
            val bar = Modifier
                .fillMaxWidth()
                .height(day.outcome.barHeight())
                .clip(shape)
            when (day.outcome) {
                ComplianceOutcome.UNLOGGED -> Box(bar.border(dimens.borderThin, scheme.outline, shape))
                else -> Box(bar.background(day.outcome.color()))
            }
        }
        Text(
            text = letter,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun LegendItem(outcome: ComplianceOutcome) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
        val dot = Modifier
            .size(LegendDotSize)
            .clip(CircleShape)
        if (outcome == ComplianceOutcome.UNLOGGED) {
            Box(dot.border(dimens.borderThin, scheme.outline, CircleShape))
        } else {
            Box(dot.background(outcome.color()))
        }
        Text(text = stringResource(outcome.legendRes()), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

private fun ComplianceOutcome.barHeight(): Dp = when (this) {
    ComplianceOutcome.MET -> BarMetHeight
    ComplianceOutcome.EXCEEDED -> BarMaxHeight
    ComplianceOutcome.SHORT -> BarShortHeight
    ComplianceOutcome.UNLOGGED -> BarUnloggedHeight
}

@Composable
private fun ComplianceOutcome.color(): Color = when (this) {
    ComplianceOutcome.MET -> MaterialTheme.colorScheme.primary
    ComplianceOutcome.EXCEEDED -> MaterialTheme.colorScheme.tertiary
    ComplianceOutcome.SHORT -> MaterialTheme.colorScheme.secondary
    ComplianceOutcome.UNLOGGED -> MaterialTheme.colorScheme.outline
}

private fun ComplianceOutcome.legendRes(): Int = when (this) {
    ComplianceOutcome.MET -> R.string.follow_up_legend_met
    ComplianceOutcome.EXCEEDED -> R.string.follow_up_legend_exceeded
    ComplianceOutcome.SHORT -> R.string.follow_up_legend_short
    ComplianceOutcome.UNLOGGED -> R.string.follow_up_legend_unlogged
}

/**
 * Three compact indicators in one row: self-weighing trend, days with logs and the last clinical measurement (a series
 * separate from self-weighing). The measurement's protocol («fasting, no shoes») goes on a caption under the row so it is
 * not lost.
 */
@Composable
private fun IndicatorsRow(panel: PatientMonitoringPanel) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val missing = stringResource(R.string.value_missing)
    val trend = panel.weightTrend
    val ratio = panel.loggedDays
    val measurement = panel.lastClinicalMeasurement
    val measurementDate = measurement?.let {
        DateTimeFormatter.ofPattern(stringResource(R.string.pattern_day_month), locale)
            .format(it.takenAt.atZone(ZoneId.systemDefault()).toLocalDate())
    }
    val protocol = measurement?.protocolChecks.orEmpty().map { stringResource(it.labelRes()).lowercase(locale) }
        .joinToString(stringResource(R.string.list_separator))
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            IndicatorTile(
                label = stringResource(R.string.follow_up_kpi_weight),
                description = trend?.let {
                    "${stringResource(R.string.follow_up_weight_trend)}. " +
                        pluralStringResource(R.plurals.follow_up_weight_trend_caption, it.weeks, it.weeks)
                } ?: stringResource(R.string.follow_up_weight_trend),
                value = trend?.slopeKgPerWeek?.let { signedOneDecimal(it) } ?: missing,
                caption = trend?.slopeKgPerWeek?.let {
                    stringResource(R.string.follow_up_kpi_weight_caption, trend.weeks)
                }.orEmpty(),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            IndicatorTile(
                label = stringResource(R.string.follow_up_logging),
                description = "${stringResource(R.string.follow_up_logging)} · ${stringResource(R.string.follow_up_logging_caption)}",
                value = ratio?.let { stringResource(R.string.follow_up_kpi_ratio, it.count, it.total) } ?: missing,
                caption = ratio?.let { stringResource(R.string.follow_up_kpi_days) }.orEmpty(),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            IndicatorTile(
                label = stringResource(R.string.follow_up_kpi_measurement),
                description = stringResource(R.string.follow_up_last_measurement),
                value = measurement?.let { oneDecimal(it.weightKg) } ?: missing,
                caption = measurementDate?.let { stringResource(R.string.follow_up_kpi_measurement_caption, it) }.orEmpty(),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (protocol.isNotBlank()) {
            Text(
                text = stringResource(R.string.follow_up_kpi_measurement_protocol, protocol),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** A small card: label, value in the metric style and a short caption. Reads as one phrase to a screen reader. */
@Composable
private fun IndicatorTile(
    label: String,
    description: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val spoken = listOf(description, value, caption).filter { it.isNotBlank() }.joinToString(". ")
    HealthifyCard(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken }, shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space12),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(text = value, style = HealthifyTheme.extendedTypography.metricMedium, color = scheme.onSurface)
            if (caption.isNotBlank()) {
                Text(text = caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A card whose header toggles its content. [header] sits left of the chevron; the whole header row is the 48 dp touch
 * target and announces «Show» or «Hide». The state survives rotation.
 */
@Composable
private fun Accordion(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val clickLabel = stringResource(if (expanded) R.string.follow_up_collapse else R.string.follow_up_expand)
    SectionCard(modifier = modifier, borderColor = borderColor, verticalSpacing = dimens.space8) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.touchMin)
                .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onToggle),
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            header()
            Icon(
                imageVector = HealthifyIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) CHEVRON_UP else CHEVRON_DOWN),
            )
        }
        if (expanded) content()
    }
}

/** AI summary · `tertiary` border; the AI text already arrives in the reader's language. */
@Composable
private fun AiSummaryAccordion(text: String) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(true) }
    Accordion(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        borderColor = scheme.tertiary,
        header = {
            AiBadge(label = stringResource(R.string.follow_up_ai_badge))
            Box(modifier = Modifier.weight(1f))
        },
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        Text(
            text = stringResource(R.string.follow_up_ai_review),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** "Today's diary" (read-only: the practitioner never writes or corrects entries). */
@Composable
private fun DiaryAccordion(entries: List<PanelDiaryEntry>) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(true) }
    val pending = entries.count { it.isAwaitingConfirmation }
    Accordion(
        expanded = expanded,
        onToggle = { expanded = !expanded },
        header = {
            Text(
                text = stringResource(R.string.follow_up_diary_today),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // Even collapsed, the header says there is something to review.
            if (pending > 0) {
                StatusChip(
                    type = StatusChipType.PendingConfirmation,
                    label = pluralStringResource(R.plurals.follow_up_pending_count, pending, pending),
                )
            }
        },
    ) {
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.follow_up_diary_empty),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        // "Pending confirmation" alone did not say whether to count the meal (validation interviews).
        if (pending > 0) {
            Text(
                text = stringResource(R.string.follow_up_pending_confirmation_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        entries.forEachIndexed { index, entry ->
            if (index > 0) CardDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    val name = entry.foodName ?: stringResource(R.string.follow_up_food_unnamed)
                    Text(
                        text = entry.grams?.let { stringResource(R.string.follow_up_food_portion, name, wholeGrams(it)) } ?: name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.follow_up_entry_meta,
                            entry.localTimestamp.shortTimeText(),
                            stringResource(entry.provenance.labelRes()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (entry.isAwaitingConfirmation) {
                    StatusChip(type = StatusChipType.PendingConfirmation)
                } else {
                    StatusChip(type = StatusChipType.Confirmed, label = stringResource(R.string.follow_up_entry_confirmed))
                }
            }
        }
    }
}

private fun PanelEntryProvenance.labelRes(): Int = when (this) {
    PanelEntryProvenance.PHOTO -> R.string.follow_up_provenance_photo
    PanelEntryProvenance.MANUAL -> R.string.follow_up_provenance_manual
    PanelEntryProvenance.OFF_PLAN -> R.string.follow_up_provenance_off_plan
}

private fun PanelProtocolCheck.labelRes(): Int = when (this) {
    PanelProtocolCheck.FASTING -> R.string.protocol_fasting
    PanelProtocolCheck.NO_SHOES -> R.string.protocol_no_shoes
    PanelProtocolCheck.LIGHT_CLOTHING -> R.string.protocol_light_clothing
    PanelProtocolCheck.EMPTY_BLADDER -> R.string.protocol_empty_bladder
    PanelProtocolCheck.SAME_SCALE -> R.string.protocol_same_scale
}

@Composable
private fun oneDecimal(value: Double): String = NumberFormat.getNumberInstance(currentLocale()).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}.format(value)

@Composable
private fun wholeGrams(value: Double): String =
    NumberFormat.getIntegerInstance(currentLocale()).format(Math.round(value))

@Composable
private fun signedOneDecimal(value: Double): String {
    val sign = when {
        value < 0 -> stringResource(R.string.sign_minus)
        value > 0 -> stringResource(R.string.sign_plus)
        else -> ""
    }
    return sign + oneDecimal(abs(value))
}

private val previewPanel = PatientMonitoringPanel(
    date = LocalDate.parse("2026-10-07"),
    week = PanelWeek(
        days = listOf(
            ComplianceOutcome.MET, ComplianceOutcome.MET, ComplianceOutcome.MET, ComplianceOutcome.SHORT,
            ComplianceOutcome.MET, ComplianceOutcome.UNLOGGED, ComplianceOutcome.EXCEEDED,
        ).mapIndexed { index, outcome -> PanelDay(LocalDate.parse("2026-09-28").plusDays(index.toLong()), outcome) },
        metDays = 4,
        totalDays = 7,
    ),
    loggedDays = DaysRatio(6, 7),
    weightTrend = PanelWeightTrend(4, -0.3),
    diary = listOf(
        PanelDiaryEntry(1, Instant.parse("2026-10-07T18:15:00Z"), "Lomo saltado", 300.0, PanelEntryProvenance.PHOTO, false),
        PanelDiaryEntry(2, Instant.parse("2026-10-07T19:10:00Z"), "Ceviche", 280.0, PanelEntryProvenance.PHOTO, true),
    ),
    lastClinicalMeasurement = PanelClinicalMeasurement(
        Instant.parse("2026-09-03T15:00:00Z"),
        74.2,
        listOf(PanelProtocolCheck.FASTING, PanelProtocolCheck.NO_SHOES),
    ),
)

@Preview(name = "Follow-up", widthDp = 360, heightDp = 800)
@Composable
private fun PatientFollowUpPreview() {
    HealthifyTheme {
        PatientFollowUpContent(
            state = PatientFollowUpUiState(
                isLoading = false,
                panel = previewPanel,
                aiSummary = "Cumple sus metas casi todos los días. El jueves registró menos energía de la indicada.",
            ),
            onRetry = {},
        )
    }
}

@Preview(name = "No AI summary or logs today", widthDp = 360, heightDp = 800)
@Composable
private fun PatientFollowUpNoAiPreview() {
    HealthifyTheme {
        PatientFollowUpContent(
            state = PatientFollowUpUiState(isLoading = false, panel = previewPanel.copy(diary = emptyList())),
            onRetry = {},
        )
    }
}

@Preview(name = "Loading", widthDp = 360, heightDp = 800)
@Composable
private fun PatientFollowUpLoadingPreview() {
    HealthifyTheme { PatientFollowUpContent(state = PatientFollowUpUiState(), onRetry = {}) }
}

@Preview(name = "Offline", widthDp = 360, heightDp = 800)
@Composable
private fun PatientFollowUpOfflinePreview() {
    HealthifyTheme {
        PatientFollowUpContent(
            state = PatientFollowUpUiState(isLoading = false, isOffline = true, loadFailed = true),
            onRetry = {},
        )
    }
}
