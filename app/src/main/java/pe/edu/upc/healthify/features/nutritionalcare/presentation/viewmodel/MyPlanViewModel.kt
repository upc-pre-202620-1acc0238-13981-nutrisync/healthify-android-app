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
 * PT4 · Mi plan. Compone tres contextos: las metas vigentes (Intake, con copia offline), si hay una versión sin
 * revisar (CareRelationship, `targets-read-status`) y el acuse «Ya las revisé» (`targets-acknowledgement`, F12).
 * Nunca muestra diagnóstico ni base de cálculo: `active-targets` no los trae.
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

    /** Si no se pudo leer `targets-read-status` (sin red o error), «Ya las revisé» intenta el acuse igual. */
    private var readStatusKnown = false
    private var pendingVersion: Int? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                // Al volver la conexión se reemplaza la copia por las metas del backend.
                if (online && wasOffline && !_state.value.isLoading) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    /** «Ya las revisé» (F12). Sin nada pendiente o sin conexión, solo vuelve a Inicio (el acuse queda pendiente). */
    fun onAcknowledge() {
        val current = _state.value
        val plan = current.plan ?: return
        if (current.isAcknowledging) return
        // DECISIÓN PT4: sin conexión el acuse no se encola (no es un dato del paciente); PT3.M volverá a avisar.
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
                    // Error sin copia guardada (con copia el repositorio ya la devolvió como éxito).
                    _state.update { it.copy(isLoading = false, plan = null, loadFailed = true) }
                },
            )
        }
    }
}

/** Metas vigentes → contenido de PT4: códigos a textos traducibles; restricciones legadas tal cual. */
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
