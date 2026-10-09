package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AcceptPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AwaitPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanHistoryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAdjustmentProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.AdjustProposalRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.AdjustProposalUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewDialog
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewLoadError
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemViewModel.Companion.ACTIVE_CARE_LINK_REQUIRED
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemViewModel.Companion.INVALID_PATIENT_MESSAGE
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemViewModel.Companion.PLAN_PROPOSAL_OUT_OF_SAFETY_BOUNDS
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemViewModel.Companion.PLAN_VERSION_ALREADY_SUPERSEDED
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

sealed interface AdjustProposalEvent {
    /** Plan ajustado asignado: vuelve a la bandeja con «Señal resuelta». */
    data class Assigned(val patientName: String?) : AdjustProposalEvent

    /** El ítem o la propuesta ya no están: vuelve a la bandeja. */
    data object Close : AdjustProposalEvent
}

/**
 * PR14.IA-A · Ajustar el plan propuesto por IA. Todo viene prellenado con la propuesta: metas, indicaciones (las
 * vigentes con lo que la IA agrega y quita) y el mensaje para el paciente. «Asignar plan ajustado» la acepta con las
 * ediciones (`asIs = false`); el backend solo las valida contra el piso calórico (`422`).
 *
 * DECISIÓN PR14.IA-A: el frame no dibuja todas las indicaciones; se ofrecen las 7 del catálogo como chips (las
 * propias del profesional se conservan siempre en el backend y no se tocan aquí). El mensaje para el paciente se puede
 * editar (≤ 500, NC-9).
 */
@HiltViewModel
class AdjustProposalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOpenReviewItem: GetOpenReviewItemUseCase,
    private val awaitPlanProposal: AwaitPlanProposalUseCase,
    private val getPlanHistory: GetPlanHistoryUseCase,
    private val acceptPlanProposal: AcceptPlanProposalUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val reviewItemId: Long = checkNotNull(savedStateHandle[AdjustProposalRoute.ARG_REVIEW_ITEM_ID])

    private val _state = MutableStateFlow(AdjustProposalUiState())
    val state: StateFlow<AdjustProposalUiState> = _state.asStateFlow()

    private val _events = Channel<AdjustProposalEvent>(Channel.BUFFERED)
    val events: Flow<AdjustProposalEvent> = _events.receiveAsFlow()

    private var patientName: String? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun onRetryLoad() = load()

    fun onEnergyChange(value: String) = updateField(ProposalEditField.ENERGY) { it.copy(energy = value, belowSafetyFloor = false) }

    fun onProteinChange(value: String) = updateField(ProposalEditField.PROTEIN) { it.copy(protein = value) }

    fun onCarbChange(value: String) = updateField(ProposalEditField.CARB) { it.copy(carb = value) }

    fun onFatChange(value: String) = updateField(ProposalEditField.FAT) { it.copy(fat = value) }

    fun onMessageChange(value: String) =
        updateField(ProposalEditField.MESSAGE) { it.copy(message = value.take(PatientMessage.MAX_LENGTH)) }

    fun onGuidelineToggle(code: PlanGuidelineCode, selected: Boolean) = _state.update {
        it.copy(guidelines = if (selected) it.guidelines + code else it.guidelines - code)
    }

    /** «Asignar plan ajustado». */
    fun onAssign() {
        val current = _state.value
        if (current.isAssigning || current.isLoading) return
        val validation = ProposalEditValidation.of(
            energyText = current.energy,
            proteinText = current.protein,
            carbText = current.carb,
            fatText = current.fat,
            guidelines = current.guidelines,
            message = current.message,
        )
        val edits = when (validation) {
            is ProposalEditValidation.Invalid -> {
                _state.update { it.copy(invalidFields = validation.fields) }
                return
            }
            is ProposalEditValidation.Valid -> validation.edits
        }
        _state.update { it.copy(isAssigning = true, invalidFields = emptySet(), belowSafetyFloor = false) }
        viewModelScope.launch {
            val result = acceptPlanProposal(reviewItemId, PlanAcceptance.WithEdits(edits))
            _state.update { it.copy(isAssigning = false) }
            result.onSuccess { _events.send(AdjustProposalEvent.Assigned(it.reviewItem.patientFullName ?: patientName)) }
            result.domainErrorOrNull()?.let(::onAssignFailed)
        }
    }

    fun onDialogConfirmed() {
        val dialog = _state.value.dialog
        _state.update { it.copy(dialog = null) }
        when (dialog) {
            ReviewDialog.NOT_AVAILABLE, ReviewDialog.ALREADY_RESOLVED, ReviewDialog.PLAN_CHANGED ->
                viewModelScope.launch { _events.send(AdjustProposalEvent.Close) }
            ReviewDialog.SERVER_ERROR -> onAssign()
            ReviewDialog.NO_ACTIVE_LINK, null -> Unit
        }
    }

    fun onDialogDismissed() = _state.update { it.copy(dialog = null) }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            val itemDeferred = async { getOpenReviewItem(reviewItemId) }
            val proposalDeferred = async { awaitPlanProposal(reviewItemId, maxAttempts = 1).last() }
            val itemResult = itemDeferred.await()
            val proposalResult = proposalDeferred.await()
            val item = itemResult.getOrNull()
            val proposal = (proposalResult.getOrNull() as? PlanProposalLookup.Ready)?.proposal
            if (item == null || proposal == null || !proposal.canBeAccepted) {
                val error = itemResult.domainErrorOrNull() ?: proposalResult.domainErrorOrNull()
                _state.update { it.copy(isLoading = false, loadError = error.toLoadError()) }
                return@launch
            }
            // Las indicaciones vigentes salen del historial del plan; la propuesta solo dice qué agrega y qué quita.
            val historyResult = getPlanHistory(item.patientId.value)
            val history = historyResult.getOrNull()
            if (history == null) {
                _state.update { it.copy(isLoading = false, loadError = historyResult.domainErrorOrNull().toLoadError()) }
                return@launch
            }
            val current = history.active?.guidelines.orEmpty()
                .filterIsInstance<PlanGuidelineItem.Catalog>()
                .map { it.code }
            patientName = item.patientFullName
            prefill(proposal, proposal.resultingGuidelines(current), item.patientFullName)
        }
    }

    private fun prefill(proposal: PlanAdjustmentProposal, guidelines: Set<PlanGuidelineCode>, fullName: String?) {
        _state.update {
            it.copy(
                isLoading = false,
                loadError = null,
                patientFirstName = fullName?.substringBefore(' ')?.takeIf(String::isNotBlank),
                previousEnergyKcal = proposal.currentEnergyKcal,
                energy = formatNumber(proposal.proposed.energyKcal),
                protein = formatNumber(proposal.proposed.proteinG),
                carb = formatNumber(proposal.proposed.carbG),
                fat = formatNumber(proposal.proposed.fatG),
                guidelines = guidelines,
                message = proposal.patientMessage.take(PatientMessage.MAX_LENGTH),
            )
        }
    }

    private fun onAssignFailed(error: DomainError) {
        when {
            error is DomainError.Validation && error.code == PLAN_PROPOSAL_OUT_OF_SAFETY_BOUNDS ->
                _state.update { it.copy(belowSafetyFloor = true) }
            error is DomainError.Validation && error.code == INVALID_PATIENT_MESSAGE ->
                _state.update { it.copy(invalidFields = setOf(ProposalEditField.MESSAGE)) }
            error is DomainError.Conflict && error.code == PLAN_VERSION_ALREADY_SUPERSEDED ->
                showDialog(ReviewDialog.PLAN_CHANGED)
            error is DomainError.Conflict -> showDialog(ReviewDialog.ALREADY_RESOLVED)
            error is DomainError.NotFound -> showDialog(ReviewDialog.NOT_AVAILABLE)
            error is DomainError.Forbidden && error.code == ACTIVE_CARE_LINK_REQUIRED -> showDialog(ReviewDialog.NO_ACTIVE_LINK)
            error is DomainError.Forbidden -> showDialog(ReviewDialog.NOT_AVAILABLE)
            else -> showDialog(ReviewDialog.SERVER_ERROR)
        }
    }

    private fun showDialog(dialog: ReviewDialog) = _state.update { it.copy(dialog = dialog) }

    private fun updateField(field: ProposalEditField, transform: (AdjustProposalUiState) -> AdjustProposalUiState) =
        _state.update { transform(it).copy(invalidFields = it.invalidFields - field) }

    private fun DomainError?.toLoadError(): ReviewLoadError = when (this) {
        null, is DomainError.NotFound, is DomainError.Forbidden -> ReviewLoadError.NOT_AVAILABLE
        DomainError.Network -> ReviewLoadError.OFFLINE
        else -> ReviewLoadError.GENERIC
    }

    private companion object {
        /** Valor editable: entero sin separador de miles («1650») o con un decimal en el idioma del teléfono. */
        fun formatNumber(value: Double): String =
            DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.getDefault())).format(value)
    }
}
