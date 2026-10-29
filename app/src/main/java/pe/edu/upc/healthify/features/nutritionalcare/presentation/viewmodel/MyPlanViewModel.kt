package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.carerelationship.application.usecase.AcknowledgeActiveTargetsUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetActiveTargetsUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.Macronutrient
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.isSecondary
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.toUiText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanDetails
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MyPlanEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MyPlanUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanChangeLine
import javax.inject.Inject

/**
 * My plan. Combines three contexts: the current targets (Intake, with an offline copy), whether a version is
 * unreviewed (CareRelationship, `targets-read-status`) and the "I've reviewed them" acknowledgement
 * (`targets-acknowledgement`). It never shows the diagnosis or calculation basis: `active-targets` lacks them.
 */
@HiltViewModel
class MyPlanViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getActiveTargets: GetActiveTargetsUseCase,
    private val getTargetsReadStatus: GetTargetsReadStatusUseCase,
    private val acknowledgeActiveTargets: AcknowledgeActiveTargetsUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(MyPlanUiState())
    val state: StateFlow<MyPlanUiState> = _state.asStateFlow()

    private val _events = Channel<MyPlanEvent>(Channel.BUFFERED)
    val events: Flow<MyPlanEvent> = _events.receiveAsFlow()

    /** If `targets-read-status` could not be read (offline, error), "I've reviewed them" still tries to acknowledge. */
    private var readStatusKnown = false
    private var pendingVersion: Int? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                // When the connection returns, the copy is replaced by the backend targets.
                if (online && wasOffline && !_state.value.isLoading) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    /** "I've reviewed them". With nothing pending or offline, it just goes Home (the acknowledgement stays pending). */
    fun onAcknowledge() {
        val current = _state.value
        val plan = current.plan ?: return
        if (current.isAcknowledging) return
        // Offline, the acknowledgement is not queued (it is not patient data); the home notice will show again.
        if (current.isOffline || (readStatusKnown && pendingVersion == null)) {
            viewModelScope.launch { _events.send(MyPlanEvent.NavigateBack) }
            return
        }
        _state.update { it.copy(isAcknowledging = true, showServerError = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            val result = user?.let { acknowledgeActiveTargets(it.id.value, plan.planVersion) }
            _state.update { it.copy(isAcknowledging = false) }
            if (result == null || result.isSuccess) {
                pendingVersion = null
                _events.send(MyPlanEvent.NavigateBack)
            } else {
                _state.update { it.copy(showServerError = true) }
            }
        }
    }

    fun onServerErrorRetry() {
        _state.update { it.copy(showServerError = false) }
        onAcknowledge()
    }

    fun onServerErrorDismiss() = _state.update { it.copy(showServerError = false) }

    private fun load() {
        _state.update { it.copy(isLoading = it.plan == null, loadFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            val readStatus = async { getTargetsReadStatus(user.id.value) }
            val lookup = getActiveTargets(user.id.value)
            val status = readStatus.await().getOrNull()
            readStatusKnown = status != null
            pendingVersion = status?.versionAwaitingReview
            lookup.fold(
                onSuccess = { found ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            plan = found.targets?.toPlanDetails(),
                            cachedAt = found.savedAt.takeIf { found.fromCache },
                            hasNoTargets = found.targets == null,
                            loadFailed = false,
                        )
                    }
                },
                onFailure = {
                    // Error with no saved copy (with a copy, the repository already returned it as success).
                    _state.update { it.copy(isLoading = false, plan = null, loadFailed = true) }
                },
            )
        }
    }
}

/** Current targets → My plan content: codes to translatable texts; legacy restrictions as is. */
internal fun ActiveTargets.toPlanDetails(): PlanDetails = PlanDetails(
    planVersion = planVersion,
    publishedAt = validFrom,
    energyKcal = targets.energyKcal,
    proteinG = targets.proteinG,
    carbG = targets.carbG,
    fatG = targets.fatG,
    proteinShare = targets.energyShareOf(Macronutrient.PROTEIN).toFloat(),
    carbShare = targets.energyShareOf(Macronutrient.CARB).toFloat(),
    fatShare = targets.energyShareOf(Macronutrient.FAT).toFloat(),
    guidelines = guidelines.map { it.toUiText() },
    restrictions = restrictions.map { it.toUiText() },
    changes = changesFromPrevious.map { PlanChangeLine(it.toUiText(), secondary = it.isSecondary) },
    patientMessage = patientMessage,
)
