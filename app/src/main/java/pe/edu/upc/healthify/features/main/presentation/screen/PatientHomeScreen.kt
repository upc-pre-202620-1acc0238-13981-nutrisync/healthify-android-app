package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetContent
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.GreetingTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconButton
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonBlock
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.component.weekdayDayMonthText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import pe.edu.upc.healthify.features.intake.presentation.components.CachedTargetsNotice
import pe.edu.upc.healthify.features.main.presentation.state.HomeDay
import pe.edu.upc.healthify.features.main.presentation.state.HomeFollowUp
import pe.edu.upc.healthify.features.main.presentation.state.HomeMacros
import pe.edu.upc.healthify.features.main.presentation.state.HomeNotice
import pe.edu.upc.healthify.features.main.presentation.state.HomeTargets
import pe.edu.upc.healthify.features.main.presentation.state.HomeWeek
import pe.edu.upc.healthify.features.main.presentation.state.PatientHomeUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PatientHomeViewModel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.presentation.components.homeSummaryRes
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle

// Measurements of the "Home" frame.
private val NoticeBadgeSize = 32.dp
private val NoticeBadgeIconSize = 18.dp
private const val NOTICE_MAX_LINES = 2

/** Home actions (navigation + ViewModel events). */
data class PatientHomeActions(
    val onOpenMenu: () -> Unit = {},
    val onOpenReminders: () -> Unit = {},
    val onOpenMyPlan: () -> Unit = {},
    val onOpenDiary: () -> Unit = {},
    val onRegisterMeal: () -> Unit = {},
    val onOpenHowAmIToday: () -> Unit = {},
    val onOpenFollowUp: (followUpId: Long) -> Unit = {},
    val onOpenConsistencyNotice: () -> Unit = {},
    val onConsistencyCardShown: () -> Unit = {},
    val onTargetsChangedReview: () -> Unit = {},
    val onTargetsChangedLater: () -> Unit = {},
)

/**
 * Patient home (the shell's "Home" tab).
 *
 * @param onOpenMenu ☰ · the shell's main menu.
 * @param onOpenReminders bell → Reminders.
 * @param onOpenDiary "Your week" → the shell's Diary tab.
 * @param onRegisterMeal "Log meal" (first time, no targets) and "Log now" (logging gap); in the other states the
 *   bottom bar camera button covers it.
 */
@Composable
fun PatientHomeScreen(
    onOpenMenu: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenMyPlan: () -> Unit,
    onOpenDiary: () -> Unit,
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
            onOpenMenu = onOpenMenu,
            onOpenReminders = onOpenReminders,
            onOpenMyPlan = onOpenMyPlan,
            onOpenDiary = onOpenDiary,
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
        // The frame's bell opens Reminders; the ☰ opens the shell's main menu.
        GreetingTopAppBar(
            greeting = stringResource(R.string.home_greeting, state.greetingName),
            title = stringResource(R.string.home_title),
            avatarInitial = state.avatarInitial,
            actions = {
                HealthifyIconButton(
                    icon = HealthifyIcons.Bell,
                    contentDescription = stringResource(R.string.settings_reminders),
                    onClick = actions.onOpenReminders,
                )
                HealthifyIconButton(
                    icon = HealthifyIcons.Menu,
                    contentDescription = stringResource(R.string.menu_cd_open),
                    onClick = actions.onOpenMenu,
                )
            },
        )
        val targets = state.targets
        // No scroll: with targets, Home fits on one screen and the gauge takes the leftover height. Only when it
        // does not fit (short screen or large system font) does the column scroll, as a fallback.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val fontScale = LocalDensity.current.fontScale
            // If, with what is on screen, the gauge ends up shorter than its minimum, switch to the scrolling fallback.
            // It is tried again when what is shown or the available height changes.
            var overflows by remember(maxHeight, state.activeNotice, state.weekVisible, state.howAmIDoingVisible, state.nextFollowUpVisible) {
                mutableStateOf(false)
            }
            val fits = targets != null && !state.isLoading && fontScale <= MAX_FIT_FONT_SCALE && !overflows
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (fits) Modifier else Modifier.verticalScroll(rememberScrollState()))
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                when {
                    state.isLoading -> HomeSkeleton()
                    targets == null -> {
                        if (state.isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
                        // Empty state: the frame has "Log meal" (Tonal), even though the note says "no button".
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
                    else -> HomeBody(
                        state = state,
                        targets = targets,
                        actions = actions,
                        fillHeight = fits,
                        onGaugeTooShort = { overflows = true },
                    )
                }
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

// Height of the empty-state area (576 dp in the frame, minus the screen padding).
private val EmptyStateMinHeight = 560.dp

// With a system font larger than this, Home scrolls instead of squeezing the gauge.
private const val MAX_FIT_FONT_SCALE = 1.3f

// Minimum gauge height: compact (with a notice above) or full. Below it, Home scrolls.
private val GaugeMinHeightCompact = 150.dp
private val GaugeMinHeight = 200.dp

@Composable
private fun ColumnScope.HomeBody(
    state: PatientHomeUiState,
    targets: HomeTargets,
    actions: PatientHomeActions,
    fillHeight: Boolean,
    onGaugeTooShort: () -> Unit,
) {
    when (state.activeNotice) {
        HomeNotice.CONSISTENCY -> {
            ConsistencyNoticeCard(onClick = actions.onOpenConsistencyNotice)
            // The acknowledgement is recorded once the card has been drawn (once per appearance).
            LaunchedEffect(Unit) { actions.onConsistencyCardShown() }
        }
        HomeNotice.CACHED_TARGETS -> CachedTargetsNotice(cachedAt = state.cachedAt)
        HomeNotice.LOGGING_GAP -> LoggingGapCard(onRegisterNow = actions.onRegisterMeal)
        HomeNotice.ENERGY_EXCEEDED -> EnergyExceededCard()
        null -> Unit
    }
    // With a notice above, the gauge goes compact: no scale and no footer.
    val compact = state.activeNotice != null
    val minHeightPx = with(LocalDensity.current) { (if (compact) GaugeMinHeightCompact else GaugeMinHeight).roundToPx() }
    DailyGaugeCard(
        state = state,
        targets = targets,
        compact = compact,
        onClick = actions.onOpenMyPlan,
        modifier = if (fillHeight) {
            Modifier
                .weight(1f)
                .onSizeChanged { if (it.height < minHeightPx) onGaugeTooShort() }
        } else {
            Modifier
        },
        fillHeight = fillHeight,
    )
    state.week?.takeIf { state.weekVisible }?.let { WeekCard(week = it, onClick = actions.onOpenDiary) }
    TodayAndNextCard(
        outcome = state.todayOutcome.takeIf { state.howAmIDoingVisible },
        nextFollowUp = state.nextFollowUp.takeIf { state.nextFollowUpVisible },
        onOpenHowAmIToday = actions.onOpenHowAmIToday,
        onOpenFollowUp = actions.onOpenFollowUp,
    )
}

/** Loading skeleton over the gauge, the week and the rows. */
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

/**
 * Daily gauge: a 180° arc with the energy left, the "0 · 1 260 of 1 850 kcal · 1 850" scale and what is left of
 * each macro. Tapping it opens My plan. Once the target is reached the arc is full and says the target is met
 * (no red, no reproach). Without today's reading it shows the targets (from the copy, offline).
 *
 * @param fillHeight the arc grows to fill the height Home gives it; otherwise it uses its fixed height.
 */
@Composable
private fun DailyGaugeCard(
    state: PatientHomeUiState,
    targets: HomeTargets,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val consumed = state.consumedKcal
    val ratio = consumed?.let { (it / targets.energyKcal).toFloat().coerceIn(0f, 1f) } ?: 0f
    val macrosLeft = state.macrosLeft
    HealthifyCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.weight(1f) else Modifier)
                .padding(dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            GaugeArc(
                ratio = ratio,
                modifier = if (fillHeight) Modifier.weight(1f) else Modifier.height(GaugeFixedHeight),
                scale = if (!compact && consumed != null) {
                    { GaugeScale(consumedKcal = consumed, targetKcal = targets.energyKcal) }
                } else {
                    null
                },
            ) {
                GaugeCenter(state = state, targets = targets)
            }
            if (!compact && consumed == null) {
                Text(
                    text = stringResource(R.string.home_gauge_needs_connection),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider(thickness = dimens.borderThin, color = scheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth()) {
                MacroLeft(macrosLeft?.proteinG ?: targets.proteinG, R.string.home_gauge_protein, locale, Modifier.weight(1f))
                MacroLeft(macrosLeft?.carbG ?: targets.carbG, R.string.home_gauge_carb, locale, Modifier.weight(1f))
                MacroLeft(macrosLeft?.fatG ?: targets.fatG, R.string.home_gauge_fat, locale, Modifier.weight(1f))
            }
            if (!compact) {
                Text(
                    // Without today's reading the targets are shown, not what is left.
                    text = stringResource(if (macrosLeft != null) R.string.home_gauge_footer_left else R.string.home_gauge_footer_targets),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "590 kcal left today", "target met" or, without today's reading, the target. */
@Composable
private fun GaugeCenter(state: PatientHomeUiState, targets: HomeTargets) {
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    val left = state.energyLeftKcal
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            state.energyTargetReached -> Text(
                text = stringResource(R.string.home_gauge_target_met),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            else -> {
                Text(
                    text = stringResource(R.string.format_kcal, UiText.formatNumber(left ?: targets.energyKcal, locale)),
                    style = HealthifyTheme.extendedTypography.metricLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(if (left != null) R.string.home_gauge_left_today else R.string.home_gauge_target_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "0 · 1 260 of 1 850 kcal · 1 850", with "0" and the target centered under the ends of the arc. */
@Composable
private fun GaugeScale(consumedKcal: Double, targetKcal: Double) {
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    Row(modifier = Modifier.fillMaxWidth().height(GaugeScaleHeight), verticalAlignment = Alignment.CenterVertically) {
        GaugeScaleEnd(text = UiText.formatNumber(0.0, locale))
        Text(
            text = stringResource(
                R.string.home_gauge_scale,
                UiText.formatNumber(consumedKcal, locale),
                UiText.formatNumber(targetKcal, locale),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        GaugeScaleEnd(text = UiText.formatNumber(targetKcal, locale))
    }
}

/** End of the scale: as wide as the arc stroke, with the number centered under it even if it overflows. */
@Composable
private fun GaugeScaleEnd(text: String) {
    Box(modifier = Modifier.width(GaugeThickness), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(unbounded = true),
        )
    }
}

/**
 * 180° arc: `surfaceContainer` track and `secondary` progress from left to right. Fits the available width and
 * height (the diameter never exceeds the width or twice the height, minus the [scale]); [center] sits at the
 * bottom, inside the arc, and the [scale] just below it, as wide as the arc. The whole is centered in its space.
 */
@Composable
private fun GaugeArc(
    ratio: Float,
    modifier: Modifier = Modifier,
    scale: (@Composable () -> Unit)? = null,
    center: @Composable () -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceContainer
    val progress = MaterialTheme.colorScheme.secondary
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // The rounded ends go half a stroke below the arc center: reserve that height.
        val reserved = GaugeThickness / 2 + if (scale != null) GaugeScaleHeight else 0.dp
        val diameter = minOf(maxWidth, (maxHeight - reserved) * 2, GaugeMaxDiameter).coerceAtLeast(GaugeThickness * 2)
        Column(modifier = Modifier.width(diameter), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(width = diameter, height = diameter / 2 + GaugeThickness / 2),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = GaugeThickness.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.width - stroke)
                    val style = Stroke(width = stroke, cap = StrokeCap.Round)
                    drawArc(track, HALF_START_ANGLE, HALF_CIRCLE, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = style)
                    val sweep = ratio.coerceIn(0f, 1f) * HALF_CIRCLE
                    if (sweep > 0f) {
                        drawArc(progress, HALF_START_ANGLE, sweep, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = style)
                    }
                }
                // The center text rests on the line of the ends, not on the bottom edge of the stroke, and stays inside the
                // arc's hollow: with a small arc (e.g. an expanded notice) it wraps to two lines instead of overlapping it.
                Box(
                    modifier = Modifier.padding(
                        start = GaugeThickness + GaugeCenterGap,
                        end = GaugeThickness + GaugeCenterGap,
                        bottom = GaugeThickness / 2,
                    ),
                ) { center() }
            }
            scale?.invoke()
        }
    }
}

private val GaugeThickness = 24.dp
private val GaugeCenterGap = 8.dp
private val GaugeMaxDiameter = 340.dp
private val GaugeScaleHeight = 24.dp
private val GaugeFixedHeight = GaugeMaxDiameter / 2 + GaugeThickness / 2 + GaugeScaleHeight
private const val HALF_CIRCLE = 180f
private const val HALF_START_ANGLE = 180f

/** "38 g · of protein". */
@Composable
private fun MacroLeft(grams: Double, labelRes: Int, locale: java.util.Locale, modifier: Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
 * "Your week · 5 of 7 days": the last 7 days as circles. With a log, light lime with a check; without one,
 * neutral with a border (never an alert color); today, with a `primary` border. Tapping it opens the Diary.
 */
@Composable
private fun WeekCard(week: HomeWeek, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val locale = currentLocale()
    HealthifyCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(dimens.space16), verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_week_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(R.plurals.home_week_count, week.days.size, week.loggedDays, week.days.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.days.forEach { day -> WeekDay(day, locale) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space4), verticalAlignment = Alignment.CenterVertically) {
                Icon(HealthifyIcons.Check, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(WeekLegendIconSize))
                Text(
                    text = stringResource(R.string.home_week_legend),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeekDay(day: HomeDay, locale: java.util.Locale) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val name = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val description = stringResource(if (day.isLogged) R.string.home_week_day_logged else R.string.home_week_day_unlogged, name)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.space4),
    ) {
        Box(
            modifier = Modifier
                .size(WeekDaySize)
                .clip(CircleShape)
                // Light lime (Lime200) for days with a log; without one, the card's neutral background.
                .background(if (day.isLogged) scheme.inversePrimary else scheme.surfaceContainerLowest)
                .border(
                    width = if (day.isToday) dimens.borderThick else dimens.borderThin,
                    color = if (day.isToday) scheme.primary else scheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (day.isLogged) {
                Icon(HealthifyIcons.Check, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(WeekCheckSize))
            }
        }
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

private val WeekDaySize = 28.dp
private val WeekCheckSize = 16.dp
private val WeekLegendIconSize = 14.dp

/** "How I'm doing today" and "Next appointment" share a card; each row hides when there is no data. */
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

/** "Something doesn't add up" notice → diary review. Open question, inviting tone. */
@Composable
private fun ConsistencyNoticeCard(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    HomeNoticeCard(
        icon = HealthifyIcons.Help,
        badgeColor = scheme.secondary,
        containerColor = scheme.secondaryContainer,
        borderColor = scheme.secondary,
        title = stringResource(R.string.home_consistency_title),
        body = stringResource(R.string.home_consistency_body),
        onClick = onClick,
    )
}

/** "We miss you around here": local reminder, never accusatory (*Gap Is Not A Deviation*). */
@Composable
private fun LoggingGapCard(onRegisterNow: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // No scroll: in the compact notice the whole card leads to logging (it used to be a "Log now" link).
    HomeNoticeCard(
        icon = HealthifyIcons.Heart,
        badgeColor = scheme.primary,
        containerColor = scheme.primaryContainer,
        body = stringResource(R.string.home_gap_body),
        onClick = onRegisterNow,
        onClickLabel = stringResource(R.string.home_gap_action),
    )
}

/** Daily energy target exceeded: a kind invitation, never a reproach (tomorrow is another day). */
@Composable
private fun EnergyExceededCard() {
    val scheme = MaterialTheme.colorScheme
    HomeNoticeCard(
        icon = HealthifyIcons.Heart,
        badgeColor = scheme.primary,
        containerColor = scheme.primaryContainer,
        title = stringResource(R.string.home_energy_exceeded_title),
        body = stringResource(R.string.home_energy_exceeded_body),
    )
}

/**
 * Home notice, in its compact variant (Home fits on one screen): badge, optional title and text of up to
 * two lines. With [onClick] the whole card navigates (chevron); [onClickLabel] is what the screen reader announces.
 * Without [onClick], if the text does not fit in two lines, tapping the card expands it (and collapses it again).
 */
@Composable
private fun HomeNoticeCard(
    icon: ImageVector,
    badgeColor: Color,
    containerColor: Color,
    body: String,
    title: String? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusXl)
    var expanded by rememberSaveable(body) { mutableStateOf(false) }
    var overflows by remember(body) { mutableStateOf(false) }
    val expandable = onClick == null && (overflows || expanded)
    val toggleLabel = stringResource(if (expanded) R.string.home_notice_collapse else R.string.home_notice_expand)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .elevation(dimens.elevation1, shape)
            .clip(shape)
            .background(containerColor)
            .then(if (borderColor != null) Modifier.border(dimens.borderThin, borderColor, shape) else Modifier)
            .then(
                when {
                    onClick != null ->
                        Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                    expandable -> Modifier.clickable(role = Role.Button, onClickLabel = toggleLabel) { expanded = !expanded }
                    else -> Modifier.semantics(mergeDescendants = true) {}
                },
            )
            .padding(dimens.space12),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoticeBadge(icon = icon, color = badgeColor)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            }
            Text(
                text = body,
                style = if (title != null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium,
                color = if (title != null) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else NOTICE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            )
        }
        when {
            onClick != null -> Icon(HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            // Chevron pointing down (collapsed) or up (expanded).
            expandable -> Icon(
                HealthifyIcons.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) -90f else 90f),
            )
        }
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

private val previewToday = LocalDate.parse("2026-09-21")

private val previewWeek = HomeWeek(
    days = (6 downTo 0).map { back ->
        val date = previewToday.minusDays(back.toLong())
        HomeDay(date = date, isLogged = back != 3, isToday = back == 0)
    },
)

private val previewState = PatientHomeUiState(
    greetingName = "María",
    avatarInitial = "M",
    isLoading = false,
    targets = previewTargets,
    consumedKcal = 1260.0,
    macrosLeft = HomeMacros(proteinG = 38.0, carbG = 88.0, fatG = 19.0),
    week = previewWeek,
    todayOutcome = ComplianceOutcome.MET,
    nextFollowUp = HomeFollowUp(id = 7, scheduledFor = Instant.parse("2026-09-18T15:00:00Z")),
)

@Preview(name = "Home", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomePreview() {
    HealthifyTheme { PatientHomeContent(state = previewState, actions = PatientHomeActions()) }
}

/** Figma adaptability test (200 % font): with a large font, Home scrolls. */
@Preview(name = "Home - 200 % font", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PatientHomeLargeFontPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(showConsistencyCard = true), actions = PatientHomeActions())
    }
}

@Preview(name = "Home - with the consistency notice", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeConsistencyPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(showConsistencyCard = true), actions = PatientHomeActions())
    }
}

@Preview(name = "Logging gap reminder", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeGapPreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = previewState.copy(showLoggingGapCard = true, todayOutcome = ComplianceOutcome.UNLOGGED, consumedKcal = 0.0),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "Home - without macros left (shows the targets)", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeTargetsOnlyPreview() {
    HealthifyTheme { PatientHomeContent(state = previewState.copy(macrosLeft = null), actions = PatientHomeActions()) }
}

@Preview(name = "Home - short screen (scrolling fallback)", widthDp = 360, heightDp = 560)
@Composable
private fun PatientHomeShortScreenPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(showConsistencyCard = true), actions = PatientHomeActions())
    }
}

@Preview(name = "Home - energy target exceeded", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeEnergyExceededPreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = previewState.copy(
                consumedKcal = 2140.0,
                todayOutcome = ComplianceOutcome.EXCEEDED,
                macrosLeft = HomeMacros(proteinG = 0.0, carbG = 0.0, fatG = 0.0),
            ),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "Home - loading", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeLoadingPreview() {
    HealthifyTheme {
        PatientHomeContent(state = PatientHomeUiState(greetingName = "María", avatarInitial = "M"), actions = PatientHomeActions())
    }
}

@Preview(name = "Home - first time, no targets", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeEmptyPreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = PatientHomeUiState(greetingName = "María", avatarInitial = "M", isLoading = false, hasNoTargets = true),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "Home - offline (targets from cache)", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeOfflinePreview() {
    HealthifyTheme {
        PatientHomeContent(
            state = previewState.copy(isOffline = true, cachedAt = Instant.now()),
            actions = PatientHomeActions(),
        )
    }
}

@Preview(name = "Home - targets changed", widthDp = 360, heightDp = 800)
@Composable
private fun PatientHomeTargetsChangedPreview() {
    HealthifyTheme {
        PatientHomeContent(state = previewState.copy(targetsChangedVersion = 4), actions = PatientHomeActions())
    }
}
