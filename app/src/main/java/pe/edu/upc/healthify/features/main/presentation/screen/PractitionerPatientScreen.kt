package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.presentation.screen.PatientFollowUpTab
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PatientPlanTab
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PatientSummaryTab
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.PractitionerRecordTab
import pe.edu.upc.healthify.features.nutritionalcare.presentation.screen.SummaryManagementActions
import java.time.Instant

// Figma tabs: 48 dp tall, 4 dp indicator as wide as the text.
private val TabHeight = 48.dp
private val TabIndicatorHeight = 4.dp

/** The four fixed tabs of the patient record. */
enum class PatientDetailTab(@param:StringRes val labelRes: Int) {
    Summary(R.string.patient_tab_summary),
    FollowUp(R.string.patient_tab_follow_up),
    Record(R.string.patient_tab_record),
    Plan(R.string.patient_tab_plan),
}

/** Patient record snackbars that another screen requests on return. */
sealed interface PatientSnackbarRequest {
    /** "Plan published; the patient can now see their targets." */
    data object PlanPublished : PatientSnackbarRequest

    /** "Referral recorded", with "View" (→ Record). Comes from the referral screen. */
    data class ReferralRegistered(val specialty: String) : PatientSnackbarRequest

    /** Scheduling opened from the summary: "Appointment scheduled: Thu 18 Sep, 10:00". */
    data class FollowUpScheduled(val scheduledFor: Instant) : PatientSnackbarRequest
}

/**
 * A patient's record for their practitioner: TopAppBar with the name and the Summary, Follow-up, Record and
 * Plan tabs. Combines NutritionalCare and Monitoring; each tab has its own ViewModel, which reads the patient
 * from the route.
 *
 * @param onNavigate opens the appointment flow or one of its steps (route built by NutritionalCare).
 * @param management the summary's "MANAGEMENT" actions: scheduling, referral and discharge (the root graph knows
 *   the care link).
 */
@Composable
fun PractitionerPatientScreen(
    patientId: Long,
    patientName: String,
    onBack: () -> Unit,
    onNavigate: (route: Any) -> Unit,
    snackbarRequest: PatientSnackbarRequest?,
    onSnackbarHandled: () -> Unit,
    management: SummaryManagementActions,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(PatientDetailTab.Summary) }
    val snackbarHostState = remember { SnackbarHostState() }
    val publishedText = stringResource(R.string.patient_snackbar_plan_published, patientName)
    val referralTemplate = stringResource(R.string.patient_snackbar_referral_registered)
    val viewLabel = stringResource(R.string.patient_snackbar_view)
    val scheduledText = (snackbarRequest as? PatientSnackbarRequest.FollowUpScheduled)?.let { request ->
        val locale = currentLocale()
        stringResource(
            R.string.shell_follow_up_scheduled,
            request.scheduledFor.weekdayDayMonthText().replaceFirstChar { it.lowercase(locale) },
            request.scheduledFor.shortTimeText(),
        )
    }
    LaunchedEffect(snackbarRequest) {
        val request = snackbarRequest ?: return@LaunchedEffect
        onSnackbarHandled()
        when (request) {
            PatientSnackbarRequest.PlanPublished -> {
                selectedTab = PatientDetailTab.Summary
                snackbarHostState.showSnackbar(publishedText, duration = SnackbarDuration.Short)
            }
            is PatientSnackbarRequest.ReferralRegistered -> {
                val result = snackbarHostState.showSnackbar(
                    message = referralTemplate.format(request.specialty),
                    actionLabel = viewLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) selectedTab = PatientDetailTab.Record
            }
            is PatientSnackbarRequest.FollowUpScheduled -> {
                selectedTab = PatientDetailTab.Summary
                scheduledText?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
            }
        }
    }
    PractitionerPatientContent(
        patientName = patientName,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) { tab ->
        when (tab) {
            PatientDetailTab.Summary -> PatientSummaryTab(
                patientId = patientId,
                patientName = patientName,
                onNavigate = onNavigate,
                onOpenFollowUp = { selectedTab = PatientDetailTab.FollowUp },
                management = management,
            )
            PatientDetailTab.FollowUp -> PatientFollowUpTab()
            PatientDetailTab.Record -> PractitionerRecordTab()
            PatientDetailTab.Plan -> PatientPlanTab(patientId = patientId, patientName = patientName, onNavigate = onNavigate)
        }
    }
}

@Composable
fun PractitionerPatientContent(
    patientName: String,
    selectedTab: PatientDetailTab,
    onSelectTab: (PatientDetailTab) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    tabContent: @Composable (PatientDetailTab) -> Unit,
) {
    SystemBarsAppearance(darkBackground = false)
    // Swipes between tabs: tapping a tab moves the pager and the settled page updates the selected tab.
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { PatientDetailTab.entries.size }
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) pagerState.animateScrollToPage(selectedTab.ordinal)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> onSelectTab(PatientDetailTab.entries[page]) }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HealthifyTopAppBar(title = patientName.uppercase(currentLocale()), onBack = onBack)
            PatientTabRow(selected = selectedTab, onSelect = onSelectTab)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) { tabContent(PatientDetailTab.entries[page]) }
            }
        }
        HealthifySnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Above the tab's pinned action (88 dp in the Figma).
                .padding(bottom = HealthifyTheme.dimens.space64 + HealthifyTheme.dimens.space24),
        )
    }
}

/** "Patient tabs": four of equal width, Label/Large, `primary` indicator under the selected one. */
@Composable
private fun PatientTabRow(selected: PatientDetailTab, onSelect: (PatientDetailTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            PatientDetailTab.entries.forEach { tab ->
                val isSelected = tab == selected
                var textWidth by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = TabHeight)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(top = HealthifyTheme.dimens.space12, bottom = HealthifyTheme.dimens.space12)
                            .onSizeChanged { textWidth = with(density) { it.width.toDp() } },
                    )
                    Box(
                        modifier = Modifier
                            .width(textWidth)
                            .height(TabIndicatorHeight)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(if (isSelected) scheme.primary else scheme.surface),
                    )
                }
            }
        }
        HorizontalDivider(thickness = HealthifyTheme.dimens.borderThin, color = scheme.outlineVariant)
    }
}

@Preview(name = "Patient record (tabs)", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerPatientPreview() {
    HealthifyTheme {
        PractitionerPatientContent(
            patientName = "Ana Flores",
            selectedTab = PatientDetailTab.Summary,
            onSelectTab = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) { }
    }
}
