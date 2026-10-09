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
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedDiaryEntriesUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.main.presentation.state.HomeFollowUp
import pe.edu.upc.healthify.features.main.presentation.state.HomeTargets
import pe.edu.upc.healthify.features.main.presentation.state.PatientHomeUiState
import pe.edu.upc.healthify.features.monitoring.application.usecase.AcknowledgeConsistencyPromptUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.DetectLoggingGapUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetConsistencyIndexUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetTodayProgressUseCase
import java.time.Clock
import javax.inject.Inject

/**
 * PT3 · Inicio. Compone cuatro contextos con sus use cases: las metas vigentes (Intake, con copia offline), cómo va
 * hoy, el índice de consistencia y la próxima consulta (Monitoring) y si hay metas nuevas sin revisar
 * (CareRelationship). Las metas mandan: sin ellas la pantalla es PT3.V; lo demás se pinta a medida que llega y,
 * si falla, simplemente no aparece (nunca bloquea Inicio).
 *
 * Se refresca al crearse, al volver a la pantalla ([onScreenResumed]), al recuperar la conexión y cuando el backend
 * acepta comidas de la cola.
 */
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getActiveTargets: GetActiveTargetsUseCase,
    private val getTodayProgress: GetTodayProgressUseCase,
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

    /** MA-7: hay un aviso de consistencia que el paciente aún no vio; se acusa una sola vez al pintarlo. */
    private var consistencyPromptPending = false
    private var consistencyAckInFlight = false

    /** PT3.M «Más tarde»: no se vuelve a ofrecer la misma versión mientras viva Inicio. */
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
        // Comidas de la cola aceptadas por el backend: cambian las kcal de hoy y el hueco de registro (PT18). Al volver
        // la conexión la lectura suele llegar antes que el envío, así que se relee al aceptarse.
        viewModelScope.launch { observeSyncedDiaryEntries().collect { refresh() } }
        refresh()
    }

    /** Al volver a Inicio (p. ej. desde PT4 tras «Ya las revisé»). La primera vez ya cargó `init`. */
    fun onScreenResumed() {
        if (!firstResumeSkipped) {
            firstResumeSkipped = true
            return
        }
        refresh()
    }

    /** La tarjeta «Algo no cuadra» se pintó: MA-7 registra que el paciente la vio (antes de cualquier escalación). */
    fun onConsistencyCardShown() {
        if (!consistencyPromptPending || consistencyAckInFlight) return
        consistencyAckInFlight = true
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            val acknowledged = user != null && acknowledgeConsistencyPrompt(user.id.value).isSuccess
            // Si falló, se reintenta la próxima vez que se pinte la tarjeta.
            if (acknowledged) consistencyPromptPending = false
            consistencyAckInFlight = false
        }
    }

    /** PT3.M «Más tarde» (y también al ir a PT4 con «Ver mis metas nuevas»). */
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
                // Nota PT3 «Error cache → mismo copy que Vacío».
                hasNoTargets = targets == null,
            )
        }
        // PT18 se calcula en línea; sin conexión queda el último aviso calculado (el recordatorio es local).
        if (targets == null || found.fromCache) return
        val gap = detectLoggingGap(uid, targets.validFrom.atZone(clock.zone).toLocalDate()).getOrNull()
        if (gap != null) _state.update { it.copy(showLoggingGapCard = gap) }
    }

    private suspend fun loadToday(uid: Long) {
        // Sin red se conserva lo último leído hoy (si había); el día solo se calcula en línea.
        getTodayProgress(uid).onSuccess { progress ->
            _state.update { it.copy(consumedKcal = progress.observedEnergyKcal, todayOutcome = progress.outcome) }
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
        // DECISIÓN PT3.M: «Tu nutricionista ajustó tus metas» solo desde la versión 2; la primera se acusa en PT4.
        val offer = pending?.takeIf { it >= FIRST_ADJUSTED_VERSION && it != dismissedChangedVersion }
        _state.update { it.copy(targetsChangedVersion = offer) }
    }

    private companion object {
        const val FIRST_ADJUSTED_VERSION = 2
    }
}

private fun ActiveTargets.toHomeTargets() = HomeTargets(
    planVersion = planVersion,
    energyKcal = targets.energyKcal,
    proteinG = targets.proteinG,
    carbG = targets.carbG,
    fatG = targets.fatG,
)
