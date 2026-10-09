package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AcceptPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AwaitPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.ResolveReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.ReviewItemRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ProposalSection
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewDialog
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewItemHeader
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewItemUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewLoadError
import javax.inject.Inject

sealed interface ReviewItemEvent {
    /** Resuelto (con o sin plan asignado): vuelve a la bandeja con «Señal resuelta» (PR13.1). */
    data class Resolved(val patientName: String?) : ReviewItemEvent

    /** PR14.IA «Ajustar» → PR14.IA-A. */
    data class OpenAdjust(val reviewItemId: Long) : ReviewItemEvent

    /** PR14 «Ajustar el plan ahora»: la ficha del paciente, como acción aparte (nunca automática). */
    data class OpenPatient(val patientId: Long, val careLinkId: Long, val patientName: String) : ReviewItemEvent

    /** El ítem ya no está: vuelve a la bandeja, que se relee. */
    data object Close : ReviewItemEvent
}

/**
 * PR14 · Resolver ítem (+ PR14.1) y PR14.IA · plan propuesto por IA. Compone NutritionalCare (bandeja y propuesta) con
 * CareRelationship (la cartera, para abrir la ficha desde «Ajustar el plan ahora»).
 *
 * DECISIÓN PR14.IA: además de «Ajustar» y «Resolver» (asignar tal cual), se ofrece «Resolver sin asignar este plan»,
 * que muestra el formulario de PR14: el backend lo permite (`/resolution` descarta la propuesta) y así la IA nunca es
 * el único camino para cerrar la señal. Mientras la propuesta se genera (`202`) se puede resolver sin esperarla.
 * DECISIÓN PR14: «Ajustar el plan ahora» abre la ficha del paciente (PR11 está fuera de uso); el ítem sigue abierto
 * y se resuelve después con «Sí».
 */
@HiltViewModel
class ReviewItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOpenReviewItem: GetOpenReviewItemUseCase,
    private val awaitPlanProposal: AwaitPlanProposalUseCase,
    private val resolveReviewItem: ResolveReviewItemUseCase,
    private val acceptPlanProposal: AcceptPlanProposalUseCase,
    private val getPatientRoster: GetPatientRosterUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val reviewItemId: Long = checkNotNull(savedStateHandle[ReviewItemRoute.ARG_REVIEW_ITEM_ID])

    private val _state = MutableStateFlow(ReviewItemUiState())
    val state: StateFlow<ReviewItemUiState> = _state.asStateFlow()

    private val _events = Channel<ReviewItemEvent>(Channel.BUFFERED)
    val events: Flow<ReviewItemEvent> = _events.receiveAsFlow()

    private var item: ReviewItem? = null
    private var proposalJob: Job? = null
    private var failedAction: Action? = null

    private enum class Action { RESOLVE, ACCEPT, OPEN_PATIENT }

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun onRetryLoad() = load()

    fun onAdjustedChange(adjusted: Boolean) = _state.update { it.copy(adjusted = adjusted, showOutcomeError = false) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    /** PR14.IA «Resolver sin asignar este plan» (o «Resolver sin esperar» mientras se genera). */
    fun onResolveWithoutProposal() = _state.update { it.copy(showManualResolution = true) }

    /** PR14 «Resolver»: decir si el plan se ajustó es obligatorio (PR14.1). */
    fun onResolve() {
        val current = _state.value
        if (current.isBusy) return
        val resolution = ReviewResolution.of(current.adjusted, current.note)
        if (resolution == null) {
            _state.update { it.copy(showOutcomeError = true) }
            return
        }
        _state.update { it.copy(isResolving = true) }
        viewModelScope.launch {
            val result = resolveReviewItem(reviewItemId, resolution)
            _state.update { it.copy(isResolving = false) }
            result.onSuccess { _events.send(ReviewItemEvent.Resolved(it.patientFullName ?: item?.patientFullName)) }
            result.domainErrorOrNull()?.let { error ->
                when {
                    error is DomainError.Validation && error.code == RESOLUTION_OUTCOME_REQUIRED ->
                        _state.update { it.copy(showOutcomeError = true) }
                    error is DomainError.NotFound || error is DomainError.Forbidden -> showDialog(ReviewDialog.NOT_AVAILABLE)
                    else -> showServerError(Action.RESOLVE)
                }
            }
        }
    }

    /** PR14.IA «Resolver»: asigna el plan propuesto tal cual y lo publica (acción explícita del profesional). */
    fun onAcceptAsIs() {
        if (_state.value.isBusy || _state.value.proposal !is ProposalSection.Ready) return
        _state.update { it.copy(isAccepting = true) }
        viewModelScope.launch {
            val result = acceptPlanProposal(reviewItemId, PlanAcceptance.AsIs)
            _state.update { it.copy(isAccepting = false) }
            result.onSuccess { _events.send(ReviewItemEvent.Resolved(it.reviewItem.patientFullName ?: item?.patientFullName)) }
            result.domainErrorOrNull()?.let(::onAcceptFailed)
        }
    }

    /** PR14.IA «Ajustar» → PR14.IA-A con todo prellenado. */
    fun onAdjust() {
        if (_state.value.isBusy) return
        viewModelScope.launch { _events.send(ReviewItemEvent.OpenAdjust(reviewItemId)) }
    }

    /** PR14 «Ajustar el plan ahora»: abre la ficha del paciente (el vínculo se busca en la cartera). */
    fun onAdjustPlanNow() {
        val current = item ?: return
        if (_state.value.isBusy) return
        _state.update { it.copy(isOpeningPatient = true) }
        viewModelScope.launch {
            val practitionerId = observeCurrentUser().first()?.id?.value
            val result = practitionerId?.let { getPatientRoster(it) }
            _state.update { it.copy(isOpeningPatient = false) }
            when {
                result == null -> showDialog(ReviewDialog.NO_ACTIVE_LINK)
                result.isFailure -> showServerError(Action.OPEN_PATIENT)
                else -> {
                    val patient = result.getOrThrow().patients
                        .firstOrNull { it.patientId == current.patientId.value && it.isLinkActive }
                    if (patient == null) {
                        showDialog(ReviewDialog.NO_ACTIVE_LINK)
                    } else {
                        _events.send(ReviewItemEvent.OpenPatient(patient.patientId, patient.careLinkId.value, patient.fullName))
                    }
                }
            }
        }
    }

    fun onDialogConfirmed() {
        val dialog = _state.value.dialog
        _state.update { it.copy(dialog = null) }
        when (dialog) {
            ReviewDialog.NOT_AVAILABLE, ReviewDialog.ALREADY_RESOLVED ->
                viewModelScope.launch { _events.send(ReviewItemEvent.Close) }
            ReviewDialog.PLAN_CHANGED -> load()
            ReviewDialog.SERVER_ERROR -> when (failedAction) {
                Action.RESOLVE -> onResolve()
                Action.ACCEPT -> onAcceptAsIs()
                Action.OPEN_PATIENT -> onAdjustPlanNow()
                null -> Unit
            }
            ReviewDialog.NO_ACTIVE_LINK, null -> Unit
        }
    }

    fun onDialogDismissed() = _state.update { it.copy(dialog = null) }

    private fun load() {
        proposalJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            val result = getOpenReviewItem(reviewItemId)
            val found = result.getOrNull()
            if (found == null) {
                val error = result.domainErrorOrNull()
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadError = when {
                            result.isSuccess -> ReviewLoadError.NOT_AVAILABLE
                            error == DomainError.Network -> ReviewLoadError.OFFLINE
                            else -> ReviewLoadError.GENERIC
                        },
                    )
                }
                return@launch
            }
            item = found
            _state.update {
                it.copy(
                    isLoading = false,
                    header = ReviewItemHeader.of(found),
                    proposal = if (found.offersPlanProposal) ProposalSection.Generating else ProposalSection.NotOffered,
                    showManualResolution = false,
                )
            }
            if (found.offersPlanProposal) watchProposal()
        }
    }

    /** `202` → «la IA está preparando…» y se reintenta tras `Retry-After`; `404` o un fallo → PR14 normal. */
    private fun watchProposal() {
        proposalJob?.cancel()
        proposalJob = viewModelScope.launch {
            awaitPlanProposal(reviewItemId).collect { result ->
                val section = when (val lookup = result.getOrNull()) {
                    is PlanProposalLookup.Ready ->
                        if (lookup.proposal.canBeAccepted) ProposalSection.Ready(lookup.proposal) else ProposalSection.NotOffered
                    is PlanProposalLookup.Generating -> ProposalSection.Generating
                    PlanProposalLookup.None, null -> ProposalSection.NotOffered
                }
                _state.update { it.copy(proposal = section) }
            }
        }
    }

    private fun onAcceptFailed(error: DomainError) {
        when {
            error is DomainError.Conflict && error.code == PLAN_VERSION_ALREADY_SUPERSEDED -> showDialog(ReviewDialog.PLAN_CHANGED)
            error is DomainError.Conflict && error.code == REVIEW_ITEM_NOT_SUSTAINED_DEVIATION ->
                _state.update { it.copy(proposal = ProposalSection.NotOffered) }
            error is DomainError.Conflict -> showDialog(ReviewDialog.ALREADY_RESOLVED)
            // La propuesta ya no está (purgada o descartada): PR14 sin IA.
            error is DomainError.NotFound && error.code == PLAN_PROPOSAL_NOT_FOUND ->
                _state.update { it.copy(proposal = ProposalSection.NotOffered) }
            error is DomainError.NotFound -> showDialog(ReviewDialog.NOT_AVAILABLE)
            error is DomainError.Forbidden && error.code == ACTIVE_CARE_LINK_REQUIRED -> showDialog(ReviewDialog.NO_ACTIVE_LINK)
            error is DomainError.Forbidden -> showDialog(ReviewDialog.NOT_AVAILABLE)
            else -> showServerError(Action.ACCEPT)
        }
    }

    private fun showDialog(dialog: ReviewDialog) = _state.update { it.copy(dialog = dialog) }

    private fun showServerError(action: Action) {
        failedAction = action
        showDialog(ReviewDialog.SERVER_ERROR)
    }

    internal companion object {
        const val RESOLUTION_OUTCOME_REQUIRED = "ResolutionOutcomeRequired"
        const val PLAN_PROPOSAL_NOT_FOUND = "PlanProposalNotFound"
        const val PLAN_VERSION_ALREADY_SUPERSEDED = "PlanVersionAlreadySuperseded"
        const val REVIEW_ITEM_NOT_SUSTAINED_DEVIATION = "ReviewItemNotSustainedDeviation"
        const val ACTIVE_CARE_LINK_REQUIRED = "ActiveCareLinkRequired"
        const val PLAN_PROPOSAL_OUT_OF_SAFETY_BOUNDS = "PlanProposalOutOfSafetyBounds"
        const val INVALID_PATIENT_MESSAGE = "InvalidPatientMessage"
    }
}
