package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetContent
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.GreetingTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonBlock
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.isPressed
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import pe.edu.upc.healthify.features.intake.presentation.components.CachedTargetsNotice
import pe.edu.upc.healthify.features.main.presentation.state.HomeFollowUp
import pe.edu.upc.healthify.features.main.presentation.state.HomeTargets
import pe.edu.upc.healthify.features.main.presentation.state.PatientHomeUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PatientHomeViewModel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.presentation.components.homeSummaryRes
import java.time.Instant
import kotlin.math.roundToInt

// Medidas del frame «PT3 · Inicio».
private val RingSize = 104.dp
private val RingThickness = 11.dp
private val MacroDotSize = 10.dp
private val RegisterCardHeight = 100.dp
private val RegisterIconSize = 40.dp
private val NoticeBadgeSize = 36.dp
private val NoticeBadgeIconSize = 20.dp

/** Acciones de PT3 (navegación + eventos del ViewModel). */
data class PatientHomeActions(
    val onOpenMyPlan: () -> Unit = {},
    val onRegisterMeal: () -> Unit = {},
    val onOpenHowAmIToday: () -> Unit = {},
    val onOpenFollowUp: (followUpId: Long) -> Unit = {},
    val onOpenConsistencyNotice: () -> Unit = {},
    val onConsistencyCardShown: () -> Unit = {},
    val onTargetsChangedReview: () -> Unit = {},
    val onTargetsChangedLater: () -> Unit = {},
)

/**
 * PT3 · Inicio del paciente (pestaña «Inicio» del shell).
 *
 * @param onRegisterMeal «Registrar comida» y «Registrar ahora» (PT18).
 */
@Composable
fun PatientHomeScreen(
    onOpenMyPlan: () -> Unit,
    onRegisterMeal: () -> Unit,
    onOpenHowAmIToday: () -> Unit,
    onOpenFollowUp: (followUpId: Long) -> Unit,
    onOpenConsistencyNotice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }
    PatientHomeContent(
        state = state,
        actions = PatientHomeActions(
            onOpenMyPlan = onOpenMyPlan,
            onRegisterMeal = onRegisterMeal,
            onOpenHowAmIToday = onOpenHowAmIToday,
            onOpenFollowUp = onOpenFollowUp,
            onOpenConsistencyNotice = onOpenConsistencyNotice,
            onConsistencyCardShown = viewModel::onConsistencyCardShown,
            onTargetsChangedReview = {
                viewModel.onTargetsChangedDismissed()
                onOpenMyPlan()
            },
            onTargetsChangedLater = viewModel::onTargetsChangedDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun PatientHomeContent(
    state: PatientHomeUiState,
    actions: PatientHomeActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // DECISIÓN PT3: la campana del frame no tiene destino en las Notas; se agrega con PT22 (Recordatorios).
        GreetingTopAppBar(
            greeting = stringResource(R.string.home_greeting, state.greetingName),
            title = stringResource(R.string.home_title),
            avatarInitial = state.avatarInitial,
        )
        val targets = state.targets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            when {
                state.isLoading -> HomeSkeleton()
                targets == null -> {
                    if (state.isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
                    // PT3.V · el frame trae «Registrar comida» (Tonal), aunque la nota diga «sin botón».
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        text = stringResource(R.string.home_empty_body),
                        actionLabel = stringResource(R.string.home_register_meal),
                        onAction = actions.onRegisterMeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = EmptyStateMinHeight),
                    )
                }
                else -> HomeBody(state = state, targets = targets, actions = actions)
            }
        }
    }
    if (state.targetsChangedSheetVisible) {
        val version = state.targetsChangedVersion ?: return
        HealthifyBottomSheet(onDismissRequest = actions.onTargetsChangedLater) {
            BottomSheetContent(
                title = stringResource(R.string.home_targets_changed_title),
                text = stringResource(R.string.home_targets_changed_body, version),
                primaryLabel = stringResource(R.string.home_targets_changed_primary),
                onPrimary = actions.onTargetsChangedReview,
                secondaryLabel = stringResource(R.string.home_targets_changed_later),
                onSecondary = actions.onTargetsChangedLater,
            )
        }
    }
}

// Alto del área del estado vacío de PT3.V (576 dp en el frame, menos el padding de la pantalla).
private val EmptyStateMinHeight = 560.dp

@Composable
private fun HomeBody(state: PatientHomeUiState, targets: HomeTargets, actions: PatientHomeActions) {
    if (state.consistencyCardVisible) {
        ConsistencyNoticeCard(onClick = actions.onOpenConsistencyNotice)
        // MA-7: el acuse se registra cuando la tarjeta se pintó (una vez por aparición).
        LaunchedEffect(Unit) { actions.onConsistencyCardShown() }
    }
    if (state.loggingGapCardVisible) LoggingGapCard(onRegisterNow = actions.onRegisterMeal)
    if (state.isOffline || state.cachedAt != null) CachedTargetsNotice(cachedAt = state.cachedAt)
    TargetsCard(targets = targets, consumedKcal = state.consumedKcal, onSeeDetail = actions.onOpenMyPlan)
    MacroTargets(targets = targets)
    RegisterMealCard(compact = state.compactRegisterButton, onClick = actions.onRegisterMeal)
    TodayAndNextCard(
        outcome = state.todayOutcome.takeIf { state.howAmIDoingVisible },
        nextFollowUp = state.nextFollowUp.takeIf { state.nextFollowUpVisible },
        onOpenHowAmIToday = actions.onOpenHowAmIToday,
        onOpenFollowUp = actions.onOpenFollowUp,
    )
}

/** PT3.L · skeleton sobre la tarjeta de metas, los macros y las filas. */
@Composable
private fun HomeSkeleton() {
    val dimens = HealthifyTheme.dimens
    SkeletonCard()
    Column(
        modifier = Modifier.padding(vertical = dimens.space12),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space12)) {
                repeat(3) { SkeletonBlock(height = dimens.space24, modifier = Modifier.weight(1f)) }
            }
        }
    }
    SkeletonListItem()
    SkeletonListItem()
}

@Composable
private fun TargetsCard(targets: HomeTargets, consumedKcal: Double?, onSeeDetail: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val target = UiText.formatNumber(targets.energyKcal, locale)
    val ratio = consumedKcal?.let { (it / targets.energyKcal).toFloat() }
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space16)) {
            Text(
                text = stringResource(R.string.home_targets_title),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space16), verticalAlignment = Alignment.CenterVertically) {
                EnergyRing(ratio = ratio)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
                    Column(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(dimens.space4),
                    ) {
                        Text(
                            text = if (consumedKcal != null) {
                                stringResource(R.string.home_energy_progress, UiText.formatNumber(consumedKcal, locale), target)
                            } else {
                                stringResource(R.string.format_kcal, target)
                            },
                            style = HealthifyTheme.extendedTypography.metricMedium,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = when {
                                consumedKcal == null -> stringResource(R.string.home_progress_offline)
                                consumedKcal < targets.energyKcal -> stringResource(
                                    R.string.home_remaining,
                                    UiText.formatNumber(targets.energyKcal - consumedKcal, locale),
                                )
                                else -> stringResource(R.string.home_target_reached)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    HealthifyButton(
                        text = stringResource(R.string.home_see_detail),
                        onClick = onSeeDetail,
                        style = HealthifyButtonStyle.Text,
                    )
                }
            }
        }
    }
}

/** Anillo de energía: pista `surfaceContainer` y avance `secondary` desde las 12, sentido horario. */
@Composable
private fun EnergyRing(ratio: Float?) {
    val track = MaterialTheme.colorScheme.surfaceContainer
    val progress = MaterialTheme.colorScheme.secondary
    Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val stroke = RingThickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, FULL_CIRCLE, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            val sweep = (ratio ?: 0f).coerceIn(0f, 1f) * FULL_CIRCLE
            if (sweep > 0f) {
                drawArc(progress, START_ANGLE, sweep, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            }
        }
        if (ratio != null) {
            Text(
                text = stringResource(R.string.format_percent, (ratio * PERCENT).roundToInt()),
                style = HealthifyTheme.extendedTypography.metricMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val PERCENT = 100

@Composable
private fun MacroTargets(targets: HomeTargets) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = stringResource(R.string.home_today_targets),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
        HealthifyCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(dimens.space16), horizontalArrangement = Arrangement.spacedBy(dimens.space12)) {
                MacroColumn(targets.proteinG, R.string.macro_protein, scheme.secondary, locale, Modifier.weight(1f))
                MacroColumn(targets.carbG, R.string.macro_carb, scheme.tertiary, locale, Modifier.weight(1f))
                MacroColumn(targets.fatG, R.string.macro_fat, scheme.primary, locale, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroColumn(grams: Double, labelRes: Int, dotColor: Color, locale: java.util.Locale, modifier: Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4),
    ) {
        Box(
            modifier = Modifier
                .size(MacroDotSize)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = stringResource(R.string.format_grams, UiText.formatNumber(grams, locale)),
            style = HealthifyTheme.extendedTypography.metricMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * «Registrar comida»: tarjeta `secondary` de 100 dp (ícono arriba) o, con un aviso arriba, fila horizontal de 56 dp.
 */
@Composable
private fun RegisterMealCard(compact: Boolean, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusXl)
    val source = remember { MutableInteractionSource() }
    val container = if (source.isPressed(false)) HealthifyTheme.extendedColors.secondaryPressed else scheme.secondary
    val modifier = Modifier
        .fillMaxWidth()
        .elevation(dimens.elevation1, shape)
        .clip(shape)
        .background(container)
        .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
    val label = stringResource(R.string.home_register_meal)
    if (compact) {
        Row(
            modifier = modifier
                // Alto mínimo (no fijo): con la fuente al 200 % el texto puede ocupar dos líneas.
                .heightIn(min = dimens.button)
                .padding(horizontal = dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space12, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HealthifyIcons.Camera, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(dimens.space32))
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        }
    } else {
        Column(
            modifier = modifier
                .heightIn(min = RegisterCardHeight)
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space8, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(HealthifyIcons.Camera, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(RegisterIconSize))
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        }
    }
}

/** Fase 15: «Cómo voy hoy» y «Próxima consulta» comparten una card; cada fila se oculta si no hay dato. */
@Composable
private fun TodayAndNextCard(
    outcome: ComplianceOutcome?,
    nextFollowUp: HomeFollowUp?,
    onOpenHowAmIToday: () -> Unit,
    onOpenFollowUp: (Long) -> Unit,
) {
    if (outcome == null && nextFollowUp == null) return
    val dimens = HealthifyTheme.dimens
    HealthifyCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = dimens.space16, vertical = dimens.space8)) {
            if (outcome != null) {
                NavigationRow(
                    icon = HealthifyIcons.Trend,
                    title = stringResource(R.string.home_how_am_i_doing),
                    supporting = stringResource(outcome.homeSummaryRes),
                    onClick = onOpenHowAmIToday,
                )
            }
            if (outcome != null && nextFollowUp != null) {
                HorizontalDivider(
                    thickness = dimens.borderThin,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = dimens.space4),
                )
            }
            if (nextFollowUp != null) {
                NavigationRow(
                    icon = HealthifyIcons.Calendar,
                    title = stringResource(R.string.home_next_consultation),
                    supporting = stringResource(
                        R.string.format_separator_dot,
                        nextFollowUp.scheduledFor.weekdayDayMonthText(),
                        nextFollowUp.scheduledFor.shortTimeText(),
                    ),
                    onClick = { onOpenFollowUp(nextFollowUp.id) },
                )
            }
        }
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, supporting: String, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = dimens.space8),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurface)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Icon(HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurface)
    }
}

/** Aviso «Algo no cuadra» → PT16. Pregunta abierta, tono de invitación. */
@Composable
private fun ConsistencyNoticeCard(onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusXl)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevation(dimens.elevation1, shape)
            .clip(shape)
            .background(scheme.secondaryContainer)
            .border(dimens.borderThin, scheme.secondary, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(dimens.space16),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoticeBadge(icon = HealthifyIcons.Help, color = scheme.secondary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = stringResource(R.string.home_consistency_title), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(text = stringResource(R.string.home_consistency_body), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Icon(HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

/** PT18 · Te extrañamos por acá: recordatorio local, nunca acusatorio (*Gap Is Not A Deviation*). */
@Composable
private fun LoggingGapCard(onRegisterNow: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusXl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevation(dimens.elevation1, shape)
            .clip(shape)
            .background(scheme.primaryContainer)
            .padding(dimens.space16),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space12), verticalAlignment = Alignment.CenterVertically) {
            NoticeBadge(icon = HealthifyIcons.Heart, color = scheme.primary)
            Text(
                text = stringResource(R.string.home_gap_body),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        HealthifyButton(
            text = stringResource(R.string.home_gap_action),
            onClick = onRegisterNow,
            style = HealthifyButtonStyle.Text,
        )
    }
}

@Composable
private fun NoticeBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(NoticeBadgeSize)
            .clip(RoundedCornerShape(HealthifyTheme.dimens.radiusLg))
            .background(color)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.size(NoticeBadgeIconSize))
    }
}

private val previewTargets = HomeTargets(planVersion = 3, energyKcal = 1850.0, proteinG = 90.0, carbG = 238.0, fatG = 60.0)

private val previewState = PatientHomeUiState(
    greetingName = "María",
    avatarInitial = "M",
    isLoading = false,
    targets = previewTargets,
    consumedKcal = 1260.0,
    todayOutcome = ComplianceOutcome.MET,
    nextFollowUp = HomeFollowUp(id = 7, scheduledFor = Instant.parse("2026-09-18T15:00:00Z")),
)

@Preview(name = "PT3 · Inicio", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomePreview() {
    HealthifyTheme { PatientHomeContent(state = previewState, actions = PatientHomeActions()) }
}

/** Prueba de adaptabilidad del Figma («PT3 · prueba fuente 200 %»): todo sigue alcanzable con scroll. */
@Preview(name = "PT3 · Inicio — fuente 200 %", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PatientHomeLargeFontPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(showConsistencyCard = true), actions = PatientHomeActions())
    }
}

@Preview(name = "PT3 · Inicio — con aviso «Algo no cuadra»", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeConsistencyPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(showConsistencyCard = true), actions = PatientHomeActions())
    }
}

@Preview(name = "PT18 · Te extrañamos por acá", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeGapPreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = previewState.copy(showLoggingGapCard = true, todayOutcome = ComplianceOutcome.UNLOGGED, consumedKcal = 0.0),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "PT3.L · Inicio — cargando", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeLoadingPreview() {
    HealthifyTheme {
        PatientHomeContent(state = PatientHomeUiState(greetingName = "María", avatarInitial = "M"), actions = PatientHomeActions())
    }
}

@Preview(name = "PT3.V · Inicio — primera vez, sin metas", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeEmptyPreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = PatientHomeUiState(greetingName = "María", avatarInitial = "M", isLoading = false, hasNoTargets = true),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "PT3.O · Inicio — sin conexión (metas desde caché)", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeOfflinePreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = previewState.copy(isOffline = true, cachedAt = Instant.now()),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "PT3.M · Inicio — «Tus metas cambiaron»", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeTargetsChangedPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(targetsChangedVersion = 4), actions = PatientHomeActions())
    }
}
