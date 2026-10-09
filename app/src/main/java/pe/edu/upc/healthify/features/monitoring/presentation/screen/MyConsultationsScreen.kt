package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.PastConsultation
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.presentation.components.NextConsultationCard
import pe.edu.upc.healthify.features.monitoring.presentation.components.dayMonthText
import pe.edu.upc.healthify.features.monitoring.presentation.components.longDateText
import pe.edu.upc.healthify.features.monitoring.presentation.components.openAddToCalendar
import pe.edu.upc.healthify.features.monitoring.presentation.components.toUiText
import pe.edu.upc.healthify.features.monitoring.presentation.state.CheckInCard
import pe.edu.upc.healthify.features.monitoring.presentation.state.MyConsultationsUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.MyConsultationsViewModel
import java.time.Instant
import java.time.ZoneId

data class MyConsultationsActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit = {},
    val onAddToCalendar: (NextFollowUp) -> Unit = {},
    /** «Responder» (PT25.2) o «Editar mi respuesta» (PT25.3 → PT25.2). */
    val onOpenCheckIn: (followUpId: Long) -> Unit = {},
    /** Una consulta anterior con versión del plan → PT4.1. */
    val onOpenPlanVersions: () -> Unit = {},
)

/** PT25 · Mis consultas (+ PT25.3 respuesta enviada). Se llega desde PT20 «Mis consultas» y desde PT25.1. */
@Composable
fun MyConsultationsScreen(
    onBack: () -> Unit,
    onOpenCheckIn: (followUpId: Long) -> Unit,
    onOpenPlanVersions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyConsultationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val calendarHandler = rememberAddToCalendarHandler(snackbarHostState)
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    MyConsultationsContent(
        state = state,
        actions = MyConsultationsActions(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onAddToCalendar = calendarHandler,
            onOpenCheckIn = onOpenCheckIn,
            onOpenPlanVersions = onOpenPlanVersions,
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** «Agregar a mi calendario» con el aviso si el teléfono no tiene app de calendario. */
@Composable
internal fun rememberAddToCalendarHandler(snackbarHostState: SnackbarHostState): (NextFollowUp) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.consultation_calendar_title)
    val noCalendar = stringResource(R.string.consultation_calendar_unavailable)
    val inPerson = stringResource(R.string.consultation_with_practitioner, stringResource(R.string.modality_in_person))
    val remote = stringResource(R.string.consultation_with_practitioner, stringResource(R.string.modality_remote))
    return remember(context, title, noCalendar, inPerson, remote) {
        { next ->
            val description = if (next.modality == ConsultationModality.REMOTE) remote else inPerson
            if (!context.openAddToCalendar(next, title, description)) {
                scope.launch { snackbarHostState.showSnackbar(noCalendar) }
            }
        }
    }
}

@Composable
fun MyConsultationsContent(
    state: MyConsultationsUiState,
    actions: MyConsultationsActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.consultations_title), onBack = actions.onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                when {
                    state.isLoading -> repeat(2) { SkeletonCard(modifier = Modifier.fillMaxWidth()) }
                    state.loadFailed && state.isOffline -> EmptyState(
                        title = stringResource(R.string.consultations_offline_title),
                        text = stringResource(R.string.consultations_offline_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.loadFailed -> ErrorState(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth())
                    else -> ConsultationsBody(state, actions)
                }
            }
            HealthifySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = dimens.space16, vertical = dimens.space8),
            )
        }
    }
}

@Composable
private fun ColumnScope.ConsultationsBody(state: MyConsultationsUiState, actions: MyConsultationsActions) {
    val next = state.next
    if (next != null) {
        NextConsultationCard(next = next, onAddToCalendar = { actions.onAddToCalendar(next) })
    } else {
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            EmptyState(
                title = stringResource(R.string.consultations_none_title),
                text = stringResource(R.string.consultations_none_body),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HealthifyTheme.dimens.space16),
            )
        }
    }
    when (val card = state.checkInCard) {
        CheckInCard.Hidden -> Unit
        is CheckInCard.Answer -> AnswerCheckInCard(onAnswer = { actions.onOpenCheckIn(card.followUpId) })
        is CheckInCard.Sent -> SentCheckInCard(card, onEdit = { actions.onOpenCheckIn(card.followUpId) })
    }
    if (state.suggestedQuestions.isNotEmpty()) SuggestedQuestionsCard(state.suggestedQuestions)
    if (state.past.isNotEmpty()) PastConsultationsSection(state.past, actions.onOpenPlanVersions)
}

@Composable
private fun AnswerCheckInCard(onAnswer: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Text(
                text = stringResource(R.string.consultations_check_in_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.consultations_check_in_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HealthifyButton(
                text = stringResource(R.string.consultations_check_in_answer),
                onClick = onAnswer,
                style = HealthifyButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** PT25.3 · «Le contaste a tu nutricionista cómo te fue» con «Editar mi respuesta» hasta la hora de la consulta. */
@Composable
private fun SentCheckInCard(card: CheckInCard.Sent, onEdit: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = HealthifyIcons.CheckCircle, contentDescription = null, tint = scheme.primary)
                Text(
                    text = stringResource(R.string.consultations_check_in_sent_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
            }
            val date = dayMonthText((card.editedAt ?: card.submittedAt).atZone(ZoneId.systemDefault()), locale)
            Text(
                text = stringResource(
                    if (card.editedAt != null) R.string.consultations_check_in_edited_on else R.string.consultations_check_in_sent_on,
                    date,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (card.canEdit) {
                HealthifyButton(
                    text = stringResource(R.string.consultations_check_in_edit),
                    onClick = onEdit,
                    style = HealthifyButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.consultations_check_in_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** «Prepara tu consulta» (IA-4): las preguntas van tal cual las generó la IA (ya en el idioma del lector). */
@Composable
private fun SuggestedQuestionsCard(questions: List<SuggestedQuestion>) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = scheme.tertiary,
    ) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            AiBadge(label = stringResource(R.string.ai_suggestion_badge))
            Text(
                text = stringResource(R.string.consultations_prepare_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.consultations_prepare_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            questions.forEach { question ->
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = HealthifyIcons.Help, contentDescription = null, tint = scheme.onSurfaceVariant)
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PastConsultationsSection(past: List<PastConsultation>, onOpenPlanVersions: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = stringResource(R.string.consultations_past_title),
            style = HealthifyTheme.extendedTypography.overlineSection,
            color = scheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(horizontal = dimens.space16, vertical = dimens.space4)) {
                past.forEachIndexed { index, consultation ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant, thickness = dimens.borderThin)
                    // DECISIÓN PT25: el chevron de una consulta con plan publicado abre PT4.1 (versiones del plan); sin
                    // versión no hay destino y la fila no es táctil.
                    val clickable = consultation.planVersion != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimens.touchMin)
                            .then(
                                if (clickable) Modifier.clickable(role = Role.Button, onClick = onOpenPlanVersions) else Modifier,
                            )
                            .padding(vertical = dimens.space12),
                        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = HealthifyIcons.Calendar, contentDescription = null, tint = scheme.onSurfaceVariant)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                            Text(
                                text = longDateText(consultation.date.atZone(ZoneId.systemDefault()), locale),
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onSurface,
                            )
                            consultation.label?.let { label ->
                                Text(
                                    text = label.toUiText(consultation.planVersion).asString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (clickable) {
                            Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

internal val PreviewNextFollowUp = NextFollowUp(
    id = FollowUpId(31),
    scheduledFor = Instant.parse("2026-09-18T15:00:00Z"),
    modality = ConsultationModality.IN_PERSON,
    preparation = emptyList(),
    scheduledAt = Instant.parse("2026-09-04T15:00:00Z"),
)

private val PreviewState = MyConsultationsUiState(
    isLoading = false,
    hasLoaded = true,
    next = PreviewNextFollowUp,
    checkInCard = CheckInCard.Answer(31),
    suggestedQuestions = listOf(
        SuggestedQuestion("q1", "¿Cómo armo cenas con más proteína?"),
        SuggestedQuestion("q2", "¿Puedo ajustar el plan los fines de semana?"),
        SuggestedQuestion("q3", "Me cuesta registrar las cenas, ¿qué me recomiendas?"),
    ),
    past = listOf(
        PastConsultation(2, Instant.parse("2026-09-03T15:00:00Z"), ConsultationLabel.AssessmentAndNewPlan, 3),
        PastConsultation(1, Instant.parse("2026-03-12T15:00:00Z"), ConsultationLabel.FirstConsultation, 1),
    ),
)

@Preview(name = "PT25 · Mis consultas", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsPreview() {
    HealthifyTheme { MyConsultationsContent(state = PreviewState, actions = MyConsultationsActions(onBack = {})) }
}

@Preview(name = "PT25.3 · Respuesta enviada", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsSentPreview() {
    HealthifyTheme {
        MyConsultationsContent(
            state = PreviewState.copy(
                checkInCard = CheckInCard.Sent(31, Instant.parse("2026-09-15T15:00:00Z"), editedAt = null, canEdit = true),
            ),
            actions = MyConsultationsActions(onBack = {}),
        )
    }
}

@Preview(name = "PT25.3 · Respuesta bloqueada", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsLockedPreview() {
    HealthifyTheme {
        MyConsultationsContent(
            state = PreviewState.copy(
                checkInCard = CheckInCard.Sent(31, Instant.parse("2026-09-15T15:00:00Z"), editedAt = null, canEdit = false),
                suggestedQuestions = emptyList(),
            ),
            actions = MyConsultationsActions(onBack = {}),
        )
    }
}

@Preview(name = "PT25 · Sin consulta agendada", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsEmptyPreview() {
    HealthifyTheme {
        MyConsultationsContent(
            state = MyConsultationsUiState(isLoading = false, hasLoaded = true),
            actions = MyConsultationsActions(onBack = {}),
        )
    }
}

@Preview(name = "PT25 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsLoadingPreview() {
    HealthifyTheme { MyConsultationsContent(state = MyConsultationsUiState(), actions = MyConsultationsActions(onBack = {})) }
}

@Preview(name = "PT25 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsOfflinePreview() {
    HealthifyTheme {
        MyConsultationsContent(
            state = MyConsultationsUiState(isLoading = false, isOffline = true, loadFailed = true),
            actions = MyConsultationsActions(onBack = {}),
        )
    }
}

@Preview(name = "PT25 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun MyConsultationsErrorPreview() {
    HealthifyTheme {
        MyConsultationsContent(
            state = MyConsultationsUiState(isLoading = false, loadFailed = true),
            actions = MyConsultationsActions(onBack = {}),
        )
    }
}
