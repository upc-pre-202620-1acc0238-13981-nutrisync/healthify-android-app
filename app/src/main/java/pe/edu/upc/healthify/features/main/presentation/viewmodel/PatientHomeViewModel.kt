package pe.edu.upc.healthify.features.main.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetActiveTargetsUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetDailyIntakeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.main.presentation.state.HomeDay
import pe.edu.upc.healthify.features.main.presentation.state.HomeFollowUp
import pe.edu.upc.healthify.features.main.presentation.state.HomeMacros
import pe.edu.upc.healthify.features.main.presentation.state.HomeTargets
import pe.edu.upc.healthify.features.main.presentation.state.HomeWeek
import pe.edu.upc.healthify.features.main.presentation.state.PatientHomeUiState
import pe.edu.upc.healthify.features.monitoring.application.usecase.AcknowledgeConsistencyPromptUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.DetectLoggingGapUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetConsistencyIndexUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetLoggedWeekUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetTodayProgressUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.LoggedWeek
import java.time.Clock
import javax.inject.Inject

/**
 * Home. Combines four contexts through their use cases: the current targets and what is left of each macro today
 * (Intake, targets with an offline copy), how today is going, the logged week, the consistency index and the next
 * appointment (Monitoring), and whether there are new unreviewed targets (CareRelationship). Targets lead: without
 * them the screen shows the first-time state; the rest is drawn as it arrives and, if it fails, simply does not
 * show (it never blocks Home).
 *
 * Refreshes on creation, on returning to the screen ([onScreenResumed]), when the connection comes back and when
 * the backend accepts meals from the queue.
 */
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getActiveTargets: GetActiveTargetsUseCase,
    private val getDailyIntake: GetDailyIntakeUseCase,
    private val getTodayProgress: GetTodayProgressUseCase,
    private val getLoggedWeek: GetLoggedWeekUseCase,
    private val getNextFollowUp: GetNextFollowUpUseCase,
    private val getConsistencyIndex: GetConsistencyIndexUseCase,
    private val acknowledgeConsistencyPrompt: AcknowledgeConsistencyPromptUseCase,
    private val detectLoggingGap: DetectLoggingGapUseCase,
    private val getTargetsReadStatus: GetTargetsReadStatusUseCase,
    private val observeSyncedDiaryEntries: ObserveSyncedDiaryEntriesUseCase,
    private val clock: Clock,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientHomeUiState())
    val state: StateFlow<PatientHomeUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var firstResumeSkipped = false

    /** There is a consistency notice the patient has not seen yet; it is acknowledged once, when drawn. */
    private var consistencyPromptPending = false
    private var consistencyAckInFlight = false

    /** "Later" on the targets changed notice: the same version is not offered again while Home is alive. */
    private var dismissedChangedVersion: Int? = null

    init {
        viewModelScope.launch {
            observeCurrentUser().collect { user ->
                _state.update {
                    it.copy(
                        greetingName = user?.name?.firstGivenName.orEmpty(),
                        avatarInitial = user?.name?.initial?.toString().orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) refresh()
            }
        }
        // Queued meals accepted by the backend: they change today's kcal and the logging gap. When the
        // connection comes back the reading usually arrives before the upload, so it is read again on acceptance.
        viewModelScope.launch { observeSyncedDiaryEntries().collect { refresh() } }
        refresh()
    }

    /** On returning to Home (e.g. from My plan after "I've reviewed them"). The first load already ran in `init`. */
    fun onScreenResumed() {
        if (!firstResumeSkipped) {
            firstResumeSkipped = true
            return
        }
        refresh()
    }

    /** The "Something doesn't add up" card was drawn: records that the patient saw it (before any escalation). */
    fun onConsistencyCardShown() {
        if (!consistencyPromptPending || consistencyAckInFlight) return
        consistencyAckInFlight = true
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            val acknowledged = user != null && acknowledgeConsistencyPrompt(user.id.value).isSuccess
            // If it failed, it is retried the next time the card is drawn.
            if (acknowledged) consistencyPromptPending = false
            consistencyAckInFlight = false
        }
    }

    /** "Later" on the targets changed notice (also when going to My plan with "See my new targets"). */
    fun onTargetsChangedDismissed() {
        dismissedChangedVersion = _state.value.targetsChangedVersion ?: return
        _state.update { it.copy(targetsChangedVersion = null) }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val uid = user.id.value
            _state.update { it.copy(isLoading = it.targets == null && !it.hasNoTargets) }
            coroutineScope {
                launch { loadTargetsAndGap(uid) }
                launch { loadToday(uid) }
                launch { loadMacrosLeft(uid) }
                launch { loadWeek(uid) }
                launch { loadNextFollowUp(uid) }
                launch { loadConsistency(uid) }
                launch { loadTargetsChanged(uid) }
            }
        }
    }

    private suspend fun loadTargetsAndGap(uid: Long) {
        val found = getActiveTargets(uid).getOrNull()
        val targets = found?.targets
        _state.update {
            it.copy(
                isLoading = false,
                targets = targets?.toHomeTargets(),
                cachedAt = found?.savedAt?.takeIf { found.fromCache },
                // Design note: "cache error → same copy as empty".
                hasNoTargets = targets == null,
            )
        }
        // The logging gap is computed online; offline the last computed notice stays (the reminder is local).
        if (targets == null || found.fromCache) return
        val gap = detectLoggingGap(uid, targets.validFrom.atZone(clock.zone).toLocalDate()).getOrNull()
        if (gap != null) _state.update { it.copy(showLoggingGapCard = gap) }
    }

    private suspend fun loadToday(uid: Long) {
        // Offline, the last reading from today is kept (if any); the day is only computed online.
        getTodayProgress(uid).onSuccess { progress ->
            _state.update { it.copy(consumedKcal = progress.observedEnergyKcal, todayOutcome = progress.outcome) }
        }
    }

    private suspend fun loadMacrosLeft(uid: Long) {
        // Offline (or without targets) the last reading is kept; without a reading the gauge shows the targets in grams.
        getDailyIntake(uid).onSuccess { intake ->
            _state.update {
                it.copy(macrosLeft = HomeMacros(intake.remainingProteinG, intake.remainingCarbG, intake.remainingFatG))
            }
        }
    }

    private suspend fun loadWeek(uid: Long) {
        getLoggedWeek(uid).onSuccess { week ->
            _state.update { it.copy(week = week.toHomeWeek()) }
        }
    }

    private suspend fun loadNextFollowUp(uid: Long) {
        getNextFollowUp(uid).onSuccess { next ->
            _state.update { it.copy(nextFollowUp = next?.let { f -> HomeFollowUp(f.id.value, f.scheduledFor) }) }
        }
    }

    private suspend fun loadConsistency(uid: Long) {
        val index = getConsistencyIndex(uid).getOrNull()
        consistencyPromptPending = index?.patientPromptPending == true
        _state.update { it.copy(showConsistencyCard = index?.invitesPatientToReview == true) }
    }

    private suspend fun loadTargetsChanged(uid: Long) {
        val pending = getTargetsReadStatus(uid).getOrNull()?.versionAwaitingReview
        // "Your practitioner adjusted your targets" only from version 2 on; the first one is acknowledged in My plan.
        val offer = pending?.takeIf { it >= FIRST_ADJUSTED_VERSION && it != dismissedChangedVersion }
        _state.update { it.copy(targetsChangedVersion = offer) }
    }

    private companion object {
        const val FIRST_ADJUSTED_VERSION = 2
    }
}

private fun LoggedWeek.toHomeWeek() = HomeWeek(
    days = days.map { HomeDay(date = it.date, isLogged = it.isLogged, isToday = it.date == today) },
)

private fun ActiveTargets.toHomeTargets() = HomeTargets(
    planVersion = planVersion,
    energyKcal = targets.energyKcal,
    proteinG = targets.proteinG,
    carbG = targets.carbG,
    fatG = targets.fatG,
)
