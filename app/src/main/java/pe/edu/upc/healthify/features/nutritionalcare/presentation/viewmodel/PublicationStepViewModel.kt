package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetConsultationInProgressUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.PublishConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SavePublicationDraftUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SuggestGuidelinesUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.CustomGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PublicationUiState
import javax.inject.Inject

/**
 * EV-5 · Paso 4 Indicaciones y publicación (+ EV-5.E). Restricciones e indicaciones del catálogo como chips, «Otra
 * indicación» (texto del profesional, máx. 5) y un mensaje opcional para el paciente (NC-9).
 *
 * - Re-hidrata el borrador guardado (`publication-draft`); sin borrador, las indicaciones sugeridas llegan marcadas
 *   (DECISIÓN EV-5: como el frame, que las muestra elegidas).
 * - El borrador se guarda al salir del paso con «‹» y antes de publicar, para que lo escrito no se pierda aunque se
 *   cierre la app.
 * - «Publicar y cerrar consulta» usa **una** `Idempotency-Key` por consulta (guardada en el `SavedStateHandle`): «Volver
 *   a intentarlo» (EV-5.E) la reenvía y el backend no publica dos versiones.
 */
@HiltViewModel
class PublicationStepViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getConsultationInProgress: GetConsultationInProgressUseCase,
    private val suggestGuidelines: SuggestGuidelinesUseCase,
    private val saveDraft: SavePublicationDraftUseCase,
    private val publishConsultation: PublishConsultationUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    val patientName: String = savedStateHandle.get<String>(PatientArgs.PATIENT_NAME).orEmpty()

    private val _state = MutableStateFlow(PublicationUiState())
    val state: StateFlow<PublicationUiState> = _state.asStateFlow()

    private val _events = Channel<ConsultationStepEvent>(Channel.BUFFERED)
    val events: Flow<ConsultationStepEvent> = _events.receiveAsFlow()

    private var consultationId: ConsultationId? = null

    /** Lo último que quedó guardado como borrador (no se reenvía si no cambió). */
    private var lastSavedDraft: Publication? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        load()
    }

    fun onRetry() = load()

    fun onRestrictionToggle(code: PlanRestrictionCode) = _state.update {
        it.copy(restrictions = if (code in it.restrictions) it.restrictions - code else it.restrictions + code)
    }

    fun onGuidelineToggle(code: PlanGuidelineCode) = _state.update {
        it.copy(guidelines = if (code in it.guidelines) it.guidelines - code else it.guidelines + code)
    }

    /** «Otra indicación». */
    fun onAddCustomClick() = _state.update { it.copy(showCustomField = true, customError = false) }

    fun onCustomDraftChange(text: String) = _state.update { it.copy(customDraft = text, customError = false) }

    fun onConfirmCustom() {
        val text = _state.value.customDraft.trim()
        val valid = text.length in CustomGuideline.MIN_LENGTH..CustomGuideline.MAX_LENGTH &&
            _state.value.customGuidelines.size < CustomGuideline.MAX_PER_VERSION
        if (!valid) {
            _state.update { it.copy(customError = true) }
            return
        }
        _state.update {
            it.copy(
                customGuidelines = (it.customGuidelines + text).distinct(),
                customDraft = "",
                showCustomField = false,
            )
        }
    }

    fun onRemoveCustom(text: String) = _state.update { it.copy(customGuidelines = it.customGuidelines - text) }

    fun onPatientMessageChange(text: String) = _state.update {
        it.copy(patientMessage = text, messageTooLong = text.trim().length > PatientMessage.MAX_LENGTH)
    }

    /** «‹»: guarda el borrador (si cambió) y vuelve al paso 3. */
    fun onBack() {
        val publication = currentPublication()
        val id = consultationId
        if (id != null && publication != null && publication != lastSavedDraft) {
            lastSavedDraft = publication
            // Se guarda en segundo plano: salir del paso no espera a la red.
            viewModelScope.launch { saveDraft(id, publication) }
        }
        _events.trySend(ConsultationStepEvent.Back)
    }

    fun onPublish() {
        val id = consultationId ?: return
        if (_state.value.isPublishing) return
        val publication = currentPublication() ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPublishing = true, publishFailed = false) }
            if (publication != lastSavedDraft && saveDraft(id, publication).isSuccess) lastSavedDraft = publication
            val result = publishConsultation(id, publication, idempotencyKey())
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> {
                    _state.update { it.copy(isPublishing = false) }
                    _events.send(ConsultationStepEvent.Published)
                }
                error.closesConsultation() -> {
                    _state.update { it.copy(isPublishing = false) }
                    _events.send(ConsultationStepEvent.ConsultationClosed)
                }
                // EV-5.E: lo escrito sigue en pantalla y en el borrador.
                else -> _state.update { it.copy(isPublishing = false, publishFailed = true) }
            }
        }
    }

    /** EV-5.E «Volver a intentarlo»: misma `Idempotency-Key`. */
    fun onRetryPublish() = onPublish()

    /** Cerrar EV-5.E vuelve al formulario con todo lo escrito. */
    fun onPublishFailureDismissed() = _state.update { it.copy(publishFailed = false) }

    private fun idempotencyKey(): IdempotencyKey {
        val stored = savedStateHandle.get<String>(KEY_IDEMPOTENCY)
        if (stored != null) return IdempotencyKey(stored)
        val key = IdempotencyKey.random()
        savedStateHandle[KEY_IDEMPOTENCY] = key.value
        return key
    }

    /** `null` si el mensaje es demasiado largo (el campo lo marca). */
    private fun currentPublication(): Publication? {
        val state = _state.value
        if (state.messageTooLong) return null
        return Publication(
            restrictions = state.restrictions,
            guidelines = state.guidelines,
            customGuidelines = state.customGuidelines.map(::CustomGuideline),
            patientMessage = PatientMessage.ofOptional(state.patientMessage),
        )
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(load = it.load.copy(isLoading = true, loadFailed = false)) }
            val result = getConsultationInProgress(patientId)
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        load = it.load.copy(
                            isLoading = false,
                            loadFailed = true,
                            isOffline = it.isOffline || result.domainErrorOrNull() == DomainError.Network,
                        ),
                    )
                }
                return@launch
            }
            val inProgress: Consultation = result.getOrNull() ?: run {
                _events.send(ConsultationStepEvent.ConsultationClosed)
                return@launch
            }
            if (inProgress.targets?.prescribed == null) {
                _events.send(ConsultationStepEvent.Back)
                return@launch
            }
            consultationId = inProgress.id
            val draft = inProgress.publicationDraft
            val draftCustoms = draft?.customGuidelines
                ?.filter { text -> text.trim().length in CustomGuideline.MIN_LENGTH..CustomGuideline.MAX_LENGTH }
                ?.take(CustomGuideline.MAX_PER_VERSION)
            val suggestions = suggestGuidelines(inProgress.id).getOrNull()
            _state.update {
                it.copy(
                    load = it.load.copy(isLoading = false, loadFailed = false),
                    restrictions = draft?.restrictions ?: it.restrictions,
                    guidelines = draft?.guidelines ?: suggestions?.codes?.toSet() ?: it.guidelines,
                    customGuidelines = draftCustoms ?: it.customGuidelines,
                    patientMessage = draft?.patientMessage ?: it.patientMessage,
                    suggested = suggestions?.codes.orEmpty(),
                    suggestedByAi = suggestions?.isFromAi == true,
                )
            }
            if (draft != null) {
                lastSavedDraft = Publication(
                    draft.restrictions,
                    draft.guidelines,
                    draftCustoms.orEmpty().map(::CustomGuideline),
                    PatientMessage.ofOptional(draft.patientMessage),
                )
            }
        }
    }

    private companion object {
        const val KEY_IDEMPOTENCY = "publication_idempotency_key"
    }
}
