package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.ActionRow
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.IconInfoRow
import pe.edu.upc.healthify.core.designsystem.component.MetricCard
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ComplianceRatio
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationInProgress
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SinceLastConsultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SummaryFollowUp
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.PatientHeaderCard
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.complianceText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.conditionsInline
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.headline
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.savedWhenText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.shortDateText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.slopeText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.titleRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.BaselineRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.consultationStepRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientSummaryUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientTabEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PatientSummaryViewModel
import java.time.Instant
import java.time.LocalDate

/** The summary's "MANAGEMENT" section: schedule, record a referral and discharge, screens in the root graph. */
data class SummaryManagementActions(
    val onSchedule: () -> Unit = {},
    val onReferral: () -> Unit = {},
    val onDischarge: () -> Unit = {},
)

/**
 * Summary tab of the patient profile. [onNavigate] gets the route of the baseline or of the consultation
 * step; [onOpenFollowUp] switches to the Follow-up tab ("Trend · 4 weeks").
 */
@Composable
fun PatientSummaryTab(
    patientId: Long,
    patientName: String,
    onNavigate: (route: Any) -> Unit,
    onOpenFollowUp: () -> Unit,
    management: SummaryManagementActions,
    modifier: Modifier = Modifier,
    viewModel: PatientSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    ObserveEvents(viewModel.events) { event -> onNavigate(event.toRoute(patientId, patientName)) }
    PatientSummaryContent(
        state = state,
        onPrimaryAction = viewModel::onPrimaryAction,
        onEditBaseline = viewModel::onEditBaseline,
        onOpenFollowUp = onOpenFollowUp,
        management = management,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
    if (state.showStartError) {
        ServerErrorDialog(onRetry = viewModel::onRetryStart, onDismiss = viewModel::onStartErrorDismissed)
    }
}

/** The route of the baseline or consultation step a tab asks for. */
fun PatientTabEvent.toRoute(patientId: Long, patientName: String): Any = when (this) {
    is PatientTabEvent.OpenBaseline -> BaselineRoute(patientId, patientName, editing)
    is PatientTabEvent.OpenConsultationStep -> consultationStepRoute(step, patientId, patientName)
}

@Composable
fun PatientSummaryContent(
    state: PatientSummaryUiState,
    onPrimaryAction: () -> Unit,
    onEditBaseline: () -> Unit,
    onOpenFollowUp: () -> Unit,
    management: SummaryManagementActions,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val summary = state.summary
    PatientTabLayout(
        modifier = modifier,
        bottomBar = summary?.let {
            {
                HealthifyButton(
                    text = stringResource(
                        when {
                            !it.hasBaseline -> R.string.summary_register_baseline
                            it.consultationInProgress != null -> R.string.summary_continue_consultation
                            else -> R.string.summary_start_consultation
                        },
                    ),
                    onClick = onPrimaryAction,
                    loading = state.isStartingConsultation,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        when {
            summary != null -> SummaryBody(summary, onEditBaseline, onOpenFollowUp, management, state.isOffline)
            state.isLoading -> repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
            state.loadFailed && state.isOffline -> NeedsConnection(onRetry)
            state.loadFailed -> ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth())
        }
        Box(modifier = Modifier.height(dimens.space8))
    }
}

/** What the profile tabs share: scrolling content and, if any, the action pinned at the bottom. */
@Composable
fun PatientTabLayout(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space24, end = dimens.space16, bottom = dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
            content = content,
        )
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.space16),
            ) { bottomBar() }
        }
    }
}

/** "You need a connection": the patient profile is not stored on the phone (like the caseload). */
@Composable
fun NeedsConnection(onRetry: () -> Unit) {
    EmptyState(
        title = stringResource(R.string.roster_offline_title),
        text = stringResource(R.string.patient_offline_body),
        actionLabel = stringResource(R.string.ds_retry),
        onAction = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val SKELETON_ROWS = 4

@Composable
private fun ColumnScope.SummaryBody(
    summary: PatientSummary,
    onEditBaseline: () -> Unit,
    onOpenFollowUp: () -> Unit,
    management: SummaryManagementActions,
    isOffline: Boolean,
) {
    PatientHeaderCard(
        fullName = summary.fullName,
        linkedSince = summary.linkedSince,
        activePlanVersion = summary.activePlanVersion,
        isNew = summary.isNew,
    )
    val baseline = summary.baseline
    if (baseline == null) {
        NoBaselineState()
        return
    }
    summary.consultationInProgress?.let { InProgressCard(it) }
    summary.nextFollowUp?.let { NextFollowUpCard(it) }
    BaselineCard(baseline, onEditBaseline, enabled = !isOffline)
    SinceLastConsultationSection(summary.sinceLastConsultation, onOpenFollowUp)
    ManagementSection(management, enabled = !isOffline)
}

/** "No baseline yet". */
@Composable
private fun NoBaselineState() {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.padding(top = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space8),
    ) {
        Text(
            text = stringResource(R.string.summary_no_baseline_title),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        Text(
            text = stringResource(R.string.summary_no_baseline_body),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** "Consultation in progress · Step 1 of 4 · Today's measurement · saved today". */
@Composable
private fun InProgressCard(inProgress: ConsultationInProgress) {
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.History,
            title = stringResource(R.string.summary_in_progress_title),
            supportingText = stringResource(
                R.string.summary_in_progress_body,
                inProgress.step.number,
                ConsultationStep.TOTAL,
                stringResource(inProgress.step.titleRes()),
                savedWhenText(inProgress.lastSavedAt),
            ),
        )
    }
}

@Composable
private fun NextFollowUpCard(next: SummaryFollowUp) {
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Calendar,
            title = stringResource(R.string.summary_next_follow_up),
            supportingText = stringResource(
                R.string.summary_follow_up_when,
                next.scheduledFor.weekdayDayMonthText(),
                next.scheduledFor.shortTimeText(),
            ),
        )
    }
}

@Composable
private fun BaselineCard(baseline: BaselineSummary, onEdit: () -> Unit, enabled: Boolean) {
    val conditions = baseline.conditionsInline()
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Person,
            title = stringResource(R.string.summary_baseline_title),
            supportingText = listOfNotNull(baseline.headline(), conditions)
                .joinToString(stringResource(R.string.text_separator)),
            trailing = {
                HealthifyButton(
                    text = stringResource(R.string.common_edit),
                    onClick = onEdit,
                    style = HealthifyButtonStyle.Text,
                    enabled = enabled,
                )
            },
        )
    }
}

/** "SINCE THE LAST CONSULTATION": self-weigh-in trend (never a single day's weight) and adherence. */
@Composable
private fun SinceLastConsultationSection(since: SinceLastConsultation, onOpenFollowUp: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        SectionOverline(stringResource(R.string.summary_since_last_consultation))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            MetricCard(
                label = stringResource(R.string.summary_weight_label),
                value = since.weightSlopeKgPerWeek?.let { slopeText(it) } ?: stringResource(R.string.value_missing),
                caption = pluralStringResource(R.plurals.summary_weight_caption, since.trendWeeks, since.trendWeeks),
                captionColor = MaterialTheme.colorScheme.primary,
                onClick = onOpenFollowUp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MetricCard(
                label = stringResource(R.string.summary_compliance_label),
                value = since.compliance?.let { complianceText(it) } ?: stringResource(R.string.value_missing),
                caption = if (since.compliance?.total == LAST_WEEK_DAYS) {
                    stringResource(R.string.summary_compliance_last_week)
                } else {
                    stringResource(R.string.summary_compliance_since, shortDateText(since.fromDate))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

private const val LAST_WEEK_DAYS = 7

/** "MANAGEMENT": schedule, refer and discharge. */
@Composable
private fun ManagementSection(actions: SummaryManagementActions, enabled: Boolean) {
    val dimens = HealthifyTheme.dimens
    // Offline none of the three can be sent: they show disabled.
    Column(
        modifier = if (enabled) Modifier else Modifier.alpha(DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(dimens.space8),
    ) {
        SectionOverline(stringResource(R.string.summary_management))
        SectionCard(verticalSpacing = dimens.space4) {
            ActionRow(
                icon = HealthifyIcons.Agenda,
                text = stringResource(R.string.summary_schedule),
                onClick = { if (enabled) actions.onSchedule() },
            )
            CardDivider()
            ActionRow(
                icon = HealthifyIcons.Forward,
                text = stringResource(R.string.summary_referral),
                onClick = { if (enabled) actions.onReferral() },
            )
            CardDivider()
            ActionRow(
                icon = HealthifyIcons.Info,
                text = stringResource(R.string.summary_discharge),
                onClick = { if (enabled) actions.onDischarge() },
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f

private val previewSummary = PatientSummary(
    patientId = 7,
    fullName = "Ana Flores",
    linkedSince = Instant.parse("2026-03-12T15:00:00Z"),
    isLinkActive = true,
    activePlanVersion = 3,
    baseline = BaselineSummary(BiologicalSex.FEMALE, 31, 168.0, setOf(MedicalCondition.HYPOTHYROIDISM)),
    nextFollowUp = SummaryFollowUp(4, Instant.parse("2026-09-18T15:00:00Z")),
    consultationInProgress = null,
    sinceLastConsultation = SinceLastConsultation(LocalDate.parse("2026-09-30"), -0.3, 4, ComplianceRatio(5, 7)),
)

@Preview(name = "Summary", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryPreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(isLoading = false, summary = previewSummary),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}

@Preview(name = "Summary - consultation in progress", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryInProgressPreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(
                isLoading = false,
                summary = previewSummary.copy(
                    consultationInProgress = ConsultationInProgress(
                        ConsultationId(9),
                        ConsultationStep.MEASUREMENT,
                        Instant.now(),
                    ),
                ),
            ),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}

@Preview(name = "Summary - no baseline", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryNoBaselinePreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(
                isLoading = false,
                summary = previewSummary.copy(fullName = "Luz Ramírez", activePlanVersion = null, baseline = null),
            ),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}

@Preview(name = "Summary - loading", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryLoadingPreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}

@Preview(name = "Summary - offline", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryOfflinePreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(isLoading = false, isOffline = true, loadFailed = true),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}

@Preview(name = "Summary - error", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSummaryErrorPreview() {
    HealthifyTheme {
        PatientSummaryContent(
            state = PatientSummaryUiState(isLoading = false, loadFailed = true),
            onPrimaryAction = {},
            onEditBaseline = {},
            onOpenFollowUp = {},
            management = SummaryManagementActions(),
            onRetry = {},
        )
    }
}
