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

// Pestañas del Figma: 48 dp de alto, indicador de 4 dp del ancho del texto.
private val TabHeight = 48.dp
private val TabIndicatorHeight = 4.dp

/** Las cuatro pestañas fijas de la ficha del paciente (PAC · Notas). */
enum class PatientDetailTab(@param:StringRes val labelRes: Int) {
    Summary(R.string.patient_tab_summary),
    FollowUp(R.string.patient_tab_follow_up),
    Record(R.string.patient_tab_record),
    Plan(R.string.patient_tab_plan),
}

/** Snackbars de la ficha que pide otra pantalla al volver (PAC-1-S1 y PAC-1-S3). */
sealed interface PatientSnackbarRequest {
    /** PAC-1-S1 · «Plan publicado. Ana Flores ya puede ver sus metas.» */
    data object PlanPublished : PatientSnackbarRequest

    /** PAC-1-S3 · «Derivación a Endocrinología registrada.» con «Ver» (→ Expediente). Llega desde PR16. */
    data class ReferralRegistered(val specialty: String) : PatientSnackbarRequest

    /** PR17 abierto desde PAC-1: «Consulta agendada: jue 18 sept., 10:00». */
    data class FollowUpScheduled(val scheduledFor: Instant) : PatientSnackbarRequest
}

/**
 * Ficha de un paciente para su nutricionista: TopAppBar con el nombre y las pestañas Resumen (PAC-0/PAC-1/PAC-1.C),
 * Seguimiento (PAC-2), Expediente (PAC-3) y Plan (PAC-4). Compone NutritionalCare y Monitoring; cada pestaña tiene su
 * ViewModel, que lee el paciente de la ruta.
 *
 * @param onNavigate abre EV-1 o el paso de la consulta (ruta armada por NutritionalCare).
 * @param management «GESTIÓN» de PAC-1: PR17, PR16 y PR18 (el grafo raíz conoce el vínculo).
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HealthifyTopAppBar(title = patientName.uppercase(currentLocale()), onBack = onBack)
            PatientTabRow(selected = selectedTab, onSelect = onSelectTab)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
            ) {
                tabContent(selectedTab)
            }
        }
        HealthifySnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                // Sobre la acción fija de la pestaña (88 dp del Figma).
                .padding(bottom = HealthifyTheme.dimens.space64 + HealthifyTheme.dimens.space24),
        )
    }
}

/** «Pestañas del paciente»: cuatro del mismo ancho, Label/Large, indicador `primary` bajo la elegida. */
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

@Preview(name = "PAC · Ficha del paciente (pestañas)", widthDp = 360, heightDp = 800)
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
