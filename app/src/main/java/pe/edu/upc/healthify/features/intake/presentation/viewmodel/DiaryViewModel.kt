package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.intake.application.usecase.ConsumeLoggedMealNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.DiscardMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetDiaryDayUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveLoggedMealNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObservePendingMealPhotosUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryEvent
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryItem
import pe.edu.upc.healthify.features.intake.presentation.state.DiaryUiState
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Diary. Composes Intake (day, queue, photos to analyze), CareRelationship (whether the ideas card shows) and Iam
 * (the diary's first day is the day the account was created).
 * It offers no way to delete anything (*Diary Entry Never Deleted*).
 */
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getDiaryDay: GetDiaryDayUseCase,
    private val observePendingEntries: ObservePendingDiaryEntriesUseCase,
    private val observePendingPhotos: ObservePendingMealPhotosUseCase,
    private val observeSyncedEntries: ObserveSyncedDiaryEntriesUseCase,
    private val observeLoggedMealNotice: ObserveLoggedMealNoticeUseCase,
    private val consumeLoggedMealNotice: ConsumeLoggedMealNoticeUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val getAccountCreatedOn: GetAccountCreatedOnUseCase,
    private val discardMealPhoto: DiscardMealPhotoUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryUiState(today = LocalDate.now(clock)))
    val state: StateFlow<DiaryUiState> = _state.asStateFlow()

    private val _events = Channel<DiaryEvent>(Channel.BUFFERED)
    val events: Flow<DiaryEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null
    private var day: DiaryDay? = null
    private var pending: List<PendingDiaryEntry> = emptyList()
    private var photos: List<PendingMealPhoto> = emptyList()
    private var mealIdeasEnabled = false
    private var loadJob: Job? = null
    private var resumedOnce = false

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            launch { observePendingEntries(user.id.value).collect { pending = it; publishItems() } }
            launch { observePendingPhotos(user.id.value).collect { photos = it; publishItems() } }
            launch {
                observeLoggedMealNotice().filterNotNull().collect { notice ->
                    consumeLoggedMealNotice()
                    _events.send(DiaryEvent.ShowLoggedMeal(notice))
                    // When returning from a log, the diary shows today with the new meal.
                    selectDate(LocalDate.now(clock))
                }
            }
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) {
                        load()
                        refreshAiPreferences()
                        if (_state.value.earliestDate == null) loadEarliestDate(user.id)
                    }
                }
            }
            // When the connection returns, the day read usually lands before the queue is sent: without this, the meal
            // leaves the queue and does not show in the day until the next load.
            launch { observeSyncedEntries().collect { load() } }
            refreshAiPreferences()
            load()
            loadEarliestDate(user.id)
        }
    }

    /**
     * The diary starts on the day the account was created. Without that date (offline and never read) there is no
     * limit and it is requested again when the connection returns.
     */
    private suspend fun loadEarliestDate(userId: UserId) {
        val createdOn = getAccountCreatedOn(userId).getOrNull() ?: return
        val earliest = minOf(createdOn, LocalDate.now(clock))
        _state.update { it.copy(earliestDate = earliest) }
        if (_state.value.selectedDate.isBefore(earliest)) selectDate(earliest)
    }

    fun onPreviousDay() {
        if (_state.value.canGoToPreviousDay) selectDate(_state.value.selectedDate.minusDays(1))
    }

    fun onNextDay() {
        if (_state.value.canGoToNextDay) selectDate(_state.value.selectedDate.plusDays(1))
    }

    fun onRetry() = load()

    /** "Log by hand" on a photo to analyze: it will no longer be analyzed, so it is deleted from the phone. */
    fun onRegisterPendingPhotoManually(photo: PendingMealPhoto) {
        viewModelScope.launch { discardMealPhoto(photo.filePath) }
    }

    /** On returning to the tab: "today" may have changed and what was logged from another screen must show. */
    fun onResume() {
        // The first time, `init` already loaded it.
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        val today = LocalDate.now(clock)
        if (today != _state.value.today) {
            _state.update { it.copy(today = today) }
            selectDate(today)
        } else {
            load()
        }
    }

    private fun selectDate(date: LocalDate) {
        if (date == _state.value.selectedDate && day?.date == date) {
            load()
            return
        }
        day = null
        _state.update {
            it.copy(selectedDate = date, today = LocalDate.now(clock), hasDay = false, cachedAt = null, loadFailed = false)
        }
        publishItems()
        load()
    }

    private fun load() {
        val pid = patientId ?: return
        val date = _state.value.selectedDate
        loadJob?.cancel()
        _state.update { it.copy(isLoading = day?.date != date, loadFailed = false) }
        loadJob = viewModelScope.launch {
            val result = getDiaryDay(pid, date)
            val loaded = result.getOrNull()
            val error = result.domainErrorOrNull()
            if (loaded != null) day = loaded
            _state.update {
                it.copy(
                    isLoading = false,
                    hasDay = day?.date == date,
                    cachedAt = day?.takeIf { d -> d.date == date && d.fromCache }?.savedAt,
                    isOffline = it.isOffline || error == DomainError.Network,
                    loadFailed = loaded == null && error != null && error != DomainError.Network && day?.date != date,
                )
            }
            publishItems()
        }
    }

    private suspend fun refreshAiPreferences() {
        val pid = patientId ?: return
        mealIdeasEnabled = getAiPreferences(pid).canSuggestMealIdeas
        publishItems()
    }

    private fun publishItems() {
        val date = _state.value.selectedDate
        val items = buildItems(day?.takeIf { it.date == date }, pending, photos, date)
        _state.update {
            it.copy(
                items = items,
                pendingCount = pending.count { entry -> !entry.isRejected },
                showIdeasCard = mealIdeasEnabled && it.isToday,
            )
        }
    }

    /**
     * Day order: photos to analyze and "To confirm" (they ask for an action), what waits in the queue and the rest
     * from newest to oldest. Entries from the same idea go together in one card.
     */
    private fun buildItems(
        day: DiaryDay?,
        pending: List<PendingDiaryEntry>,
        photos: List<PendingMealPhoto>,
        date: LocalDate,
    ): List<DiaryItem> {
        val ordered = day?.orderedEntries.orEmpty()
        val toConfirm = ordered.filter { it.isPendingConfirmation }.map { DiaryItem.Entry(it) }
        val rest = mutableListOf<DiaryItem>()
        val seenGroups = mutableSetOf<String>()
        ordered.filterNot { it.isPendingConfirmation }.forEach { entry ->
            val group = entry.mealGroupId
            if (group == null) {
                rest += DiaryItem.Entry(entry)
            } else if (seenGroups.add(group)) {
                rest += DiaryItem.MealGroup(group, ordered.filter { it.mealGroupId == group })
            }
        }
        val queued = pending.filter { it.localTimestamp.localDate == date }
            .sortedByDescending { it.localTimestamp.instant }
            .map { DiaryItem.Pending(it) }
        val photoItems = photos.filter { it.capturedAt.localDate == date }.map { DiaryItem.PendingPhoto(it) }
        return photoItems + toConfirm + queued + rest
    }
}
