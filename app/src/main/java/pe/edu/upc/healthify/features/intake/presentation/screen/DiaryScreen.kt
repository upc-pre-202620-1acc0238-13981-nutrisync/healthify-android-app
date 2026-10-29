package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifySnackbarHost
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.component.isToday
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.features.intake.presentation.components.DiaryEntryCard
import pe.edu.upc.healthify.features.intake.presentation.components.MealGroupCard
import pe.edu.upc.healthify.features.intake.presentation.components.PendingConfirmationCard
import pe.edu.upc.healthify.features.intake.presentation.components.PendingEntryCard
import pe.edu.upc.healthify.features.intake.presentation.components.PendingPhotoCard
import pe.edu.upc.healthify.features.intake.presentation.components.diaryDateText
import pe.edu.upc.healthify.features.intake.presentation.components.previewEntry
import pe.edu.upc.healthify.features.intake.presentation.components.previewPending
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryEvent
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryItem
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.DiaryViewModel
import java.time.Instant
import java.time.LocalDate

/** Navigation out of the diary (the screens open over the shell, in the root graph). */
data class DiaryCallbacks(
    /** "Log meal" → the camera (or manual logging if AI photo is not available). */
    val onRegisterMeal: () -> Unit,
    /** "Confirm portion" → confirmation of that entry. */
    val onConfirmPortion: (DiaryEntry) -> Unit,
    /** "See ideas" → ideas for today. */
    val onOpenIdeas: () -> Unit,
    /** "See pending" → pending to sync. */
    val onOpenPendingSync: () -> Unit,
    /** "Analyze now" on a photo taken offline → photo analysis. */
    val onAnalyzePendingPhoto: (photoId: String) -> Unit,
    /** "Log by hand" → food search. */
    val onRegisterManually: () -> Unit,
    /** ☰ · the shell's main menu. */
    val onOpenMenu: () -> Unit,
)

/** Screen actions (stateless). */
data class DiaryActions(
    val onOpenMenu: () -> Unit = {},
    val onPreviousDay: () -> Unit = {},
    val onNextDay: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRegisterMeal: () -> Unit = {},
    val onConfirmPortion: (DiaryEntry) -> Unit = {},
    val onOpenIdeas: () -> Unit = {},
    val onOpenPendingSync: () -> Unit = {},
    val onAnalyzePendingPhoto: (PendingMealPhoto) -> Unit = {},
    val onRegisterPendingPhotoManually: (PendingMealPhoto) -> Unit = {},
)

/** Diary (with its Snackbar, empty, offline and loading states). Lives in the shell's "Diary" tab. */
@Composable
fun DiaryScreen(
    callbacks: DiaryCallbacks,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val locale = currentLocale()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is DiaryEvent.ShowLoggedMeal -> scope.launch {
                snackbarHostState.showSnackbar(event.notice.message().resolve(resources, locale))
            }
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    DiaryContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = DiaryActions(
            onOpenMenu = callbacks.onOpenMenu,
            onPreviousDay = viewModel::onPreviousDay,
            onNextDay = viewModel::onNextDay,
            onRetry = viewModel::onRetry,
            onRegisterMeal = callbacks.onRegisterMeal,
            onConfirmPortion = callbacks.onConfirmPortion,
            onOpenIdeas = callbacks.onOpenIdeas,
            onOpenPendingSync = callbacks.onOpenPendingSync,
            onAnalyzePendingPhoto = { callbacks.onAnalyzePendingPhoto(it.id) },
            onRegisterPendingPhotoManually = { photo ->
                viewModel.onRegisterPendingPhotoManually(photo)
                callbacks.onRegisterManually()
            },
        ),
        modifier = modifier,
    )
}

/** "Meal logged: Lomo saltado · 320 g" or "Saved. It will be sent when you are online.". */
private fun LoggedMealNotice.message(): UiText = when (outcome) {
    MealLogOutcome.QUEUED -> UiText.of(R.string.diary_snackbar_queued)
    MealLogOutcome.LOGGED -> {
        val description = grams?.let { UiText.of(R.string.format_separator_dot, description, UiText.of(R.string.format_grams, it)) }
            ?: UiText.Raw(description)
        UiText.of(R.string.diary_snackbar_logged, description)
    }
}

@Composable
fun DiaryContent(
    state: DiaryUiState,
    actions: DiaryActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(
            title = stringResource(R.string.diary_title),
            actions = {
                HealthifyIconButton(
                    icon = HealthifyIcons.Menu,
                    contentDescription = stringResource(R.string.menu_cd_open),
                    onClick = actions.onOpenMenu,
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimens.space16,
                    top = dimens.space8,
                    end = dimens.space16,
                    bottom = dimens.space24,
                ),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                item(key = "date") {
                    DateSelector(state = state, onPrevious = actions.onPreviousDay, onNext = actions.onNextDay)
                }
                if (state.isOffline || state.cachedAt != null) {
                    item(key = "offline") {
                        OfflineNotice(state.cachedAt, state.isOffline)
                    }
                }
                if (state.pendingCount > 0) {
                    item(key = "pending-link") {
                        HealthifyButton(
                            text = stringResource(R.string.diary_see_pending, state.pendingCount),
                            onClick = actions.onOpenPendingSync,
                            style = HealthifyButtonStyle.Text,
                            icon = HealthifyIcons.Sync,
                        )
                    }
                }
                if (state.showIdeasCard) {
                    item(key = "ideas") {
                        MealIdeasCard(onClick = actions.onOpenIdeas, modifier = Modifier.padding(bottom = dimens.space12))
                    }
                }
                when {
                    state.isLoading -> items(count = SKELETON_ITEMS, key = { "skeleton-$it" }) { SkeletonListItem() }
                    state.loadFailed && state.items.isEmpty() -> item(key = "error") {
                        ErrorState(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth().padding(top = dimens.space24))
                    }
                    state.isOfflineWithoutCopy -> item(key = "offline-empty") {
                        EmptyState(
                            title = stringResource(R.string.diary_offline_title),
                            text = stringResource(R.string.diary_offline_body),
                            modifier = Modifier.fillMaxWidth().padding(top = dimens.space24),
                        )
                    }
                    state.isEmpty -> item(key = "empty") {
                        EmptyState(
                            title = stringResource(
                                if (state.isToday) R.string.diary_empty_title else R.string.diary_empty_title_other_day,
                            ),
                            text = stringResource(R.string.diary_empty_body),
                            modifier = Modifier.fillMaxWidth().padding(top = dimens.space24),
                        )
                    }
                    else -> items(items = state.items, key = { it.key }) { item ->
                        DiaryItemCard(item = item, isOffline = state.isOffline, actions = actions)
                    }
                }
            }
            HealthifySnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = dimens.space16, vertical = dimens.space8),
            )
        }
        // "Actions": button pinned above the navigation bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(dimens.space16),
        ) {
            HealthifyButton(
                text = stringResource(R.string.diary_register_meal),
                onClick = actions.onRegisterMeal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DiaryItemCard(item: DiaryItem, isOffline: Boolean, actions: DiaryActions) {
    when (item) {
        is DiaryItem.Entry -> if (item.entry.isPendingConfirmation) {
            val entry = item.entry
            PendingConfirmationCard(
                entry = entry,
                // With no proposed dish there is nothing to confirm (old entry): it is only shown.
                onConfirmPortion = if (entry.proposedFoodId != null && entry.foodName != null) {
                    { actions.onConfirmPortion(entry) }
                } else {
                    null
                },
            )
        } else {
            DiaryEntryCard(entry = item.entry)
        }
        is DiaryItem.MealGroup -> MealGroupCard(entries = item.entries)
        is DiaryItem.Pending -> PendingEntryCard(entry = item.entry)
        is DiaryItem.PendingPhoto -> PendingPhotoCard(
            photo = item.photo,
            isOffline = isOffline,
            onAnalyze = { actions.onAnalyzePendingPhoto(item.photo) },
            onRegisterManually = { actions.onRegisterPendingPhotoManually(item.photo) },
        )
    }
}

/** "‹ Today, September 9 ›": it does not go past today nor back before the account existed. */
@Composable
private fun DateSelector(state: DiaryUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HealthifyIconButton(
            icon = HealthifyIcons.ChevronLeft,
            contentDescription = stringResource(R.string.diary_cd_previous_day),
            onClick = onPrevious,
            enabled = state.canGoToPreviousDay,
        )
        Text(
            text = diaryDateText(state.selectedDate, state.today),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        HealthifyIconButton(
            icon = HealthifyIcons.ChevronRight,
            contentDescription = stringResource(R.string.diary_cd_next_day),
            onClick = onNext,
            enabled = state.canGoToNextDay,
        )
    }
}

/** "Offline…" banner and, if the day comes from the phone, since when. */
@Composable
private fun OfflineNotice(cachedAt: Instant?, isOffline: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        if (isOffline) OfflineBanner(type = OfflineBannerType.Queueable)
        if (cachedAt != null) {
            val time = if (cachedAt.isToday()) {
                cachedAt.shortTimeText()
            } else {
                stringResource(R.string.format_separator_dot, cachedAt.mediumDateText(), cachedAt.shortTimeText())
            }
            Text(
                text = stringResource(R.string.diary_cached_at, time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The AI "Ideas for today" card: `tertiary` outline, badge and "See ideas ›". */
@Composable
private fun MealIdeasCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = scheme.tertiary,
    ) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            AiBadge(label = stringResource(R.string.diary_ideas_badge))
            Text(text = stringResource(R.string.diary_ideas_title), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(text = stringResource(R.string.diary_ideas_body), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
                Text(
                    text = stringResource(R.string.diary_ideas_action),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurface)
            }
        }
    }
}

private const val SKELETON_ITEMS = 3

private val PreviewToday: LocalDate = LocalDate.parse("2026-09-09")

private fun previewItems() = listOf(
    DiaryItem.Entry(previewEntry(1, "Ceviche", "14:10", EntryProvenance.PHOTO, 280.0, 0.64, confirmed = false)),
    DiaryItem.Entry(previewEntry(2, "Avena con fruta", "07:30", EntryProvenance.MANUAL, 250.0)),
    DiaryItem.Entry(previewEntry(3, "Lomo saltado", "13:15", EntryProvenance.PHOTO, 320.0, 0.82)),
    DiaryItem.Entry(previewEntry(4, "Pizza", "20:40", EntryProvenance.MANUAL, 300.0, adherence = PlanAdherence.OFF_PLAN)),
)

@Preview(name = "Diary", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryPreview() {
    HealthifyTheme {
        DiaryContent(
            state = DiaryUiState(today = PreviewToday, isLoading = false, hasDay = true, items = previewItems(), showIdeasCard = true),
            actions = DiaryActions(),
        )
    }
}

@Preview(name = "Diary - no logs today", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryEmptyPreview() {
    HealthifyTheme {
        DiaryContent(state = DiaryUiState(today = PreviewToday, isLoading = false, hasDay = true), actions = DiaryActions())
    }
}

@Preview(name = "Diary - offline, pending to send", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryOfflinePreview() {
    HealthifyTheme {
        DiaryContent(
            state = DiaryUiState(
                today = PreviewToday,
                isLoading = false,
                isOffline = true,
                hasDay = true,
                cachedAt = Instant.now(),
                pendingCount = 1,
                items = previewItems().take(2) + DiaryItem.Pending(previewPending("Lomo saltado", "13:15")),
            ),
            actions = DiaryActions(),
        )
    }
}

@Preview(name = "Diary - loading", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryLoadingPreview() {
    HealthifyTheme { DiaryContent(state = DiaryUiState(today = PreviewToday), actions = DiaryActions()) }
}

@Preview(name = "Diary - error", widthDp = 360, heightDp = 800)
@Composable
private fun DiaryErrorPreview() {
    HealthifyTheme {
        DiaryContent(state = DiaryUiState(today = PreviewToday, isLoading = false, loadFailed = true), actions = DiaryActions())
    }
}
