package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import pe.edu.upc.healthify.features.intake.application.usecase.AnalyzeMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.DiscardMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetPendingMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.KeepPhotoMealUnconfirmedUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogPhotoMealUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.SavePendingMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PhotoConfirmation
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.presentation.navigation.MealPhotoRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.PickedFoodResult
import pe.edu.upc.healthify.features.intake.presentation.state.AnalysisFailure
import pe.edu.upc.healthify.features.intake.presentation.state.CameraPermissionStatus
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoStep
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoUiState
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

/**
 * Registro por foto (IN-7, F41): PT5 Cámara → PT6 Previsualización → PT6.1 «Viendo tu foto…» → PT7 propuesta
 * (PT7.2 / PT7.3) → PT8 ajustar. Compone CareRelationship (si la foto con IA está disponible) e Intake.
 *
 * - Sin consentimiento de IA, sin la preferencia «Reconocer comidas por foto» o con la IA apagada (`403
 *   AiConsentRequired` / `503 AiFeatureDisabled`) → registrar a mano (PT9).
 * - Sin conexión al analizar → la foto queda guardada en el teléfono para analizarla al volver la red.
 * - Nada se guarda en el diario hasta confirmar; al confirmar viaja `analysisId` + `clientEntryId` (uno por foto,
 *   el mismo en cada reintento).
 * - La foto se borra del teléfono al registrar, al elegir registrar a mano o al descartarla.
 */
@HiltViewModel
class MealPhotoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAccountCreatedOn: GetAccountCreatedOnUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val analyzeMealPhoto: AnalyzeMealPhotoUseCase,
    private val savePendingMealPhoto: SavePendingMealPhotoUseCase,
    private val getPendingMealPhoto: GetPendingMealPhotoUseCase,
    private val discardMealPhoto: DiscardMealPhotoUseCase,
    private val logPhotoMeal: LogPhotoMealUseCase,
    private val keepPhotoMealUnconfirmed: KeepPhotoMealUnconfirmedUseCase,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MealPhotoUiState(today = MealFormState.today(clock), form = MealFormState.now(clock)))
    val state: StateFlow<MealPhotoUiState> = _state.asStateFlow()

    private val _events = Channel<MealPhotoEvent>(Channel.BUFFERED)
    val events: Flow<MealPhotoEvent> = _events.receiveAsFlow()

    private val clientEntryId = ClientEntryId.random()
    private var patientId: Long? = null

    /** La foto ya quedó guardada como pendiente (no se borra al salir sin registrarla). */
    private var photoIsPending = false

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>(PickedFoodResult.KEY_ID, null).filterNotNull().collect { id ->
                val name = savedStateHandle.get<String>(PickedFoodResult.KEY_NAME)
                savedStateHandle[PickedFoodResult.KEY_ID] = null
                savedStateHandle[PickedFoodResult.KEY_NAME] = null
                if (id > 0 && !name.isNullOrBlank()) onFoodPicked(MealFood(id, name))
            }
        }
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        val user = observeCurrentUser().first() ?: return
        patientId = user.id.value
        viewModelScope.launch {
            getAccountCreatedOn(user.id).getOrNull()?.let { day -> _state.update { it.copy(earliestDay = day) } }
        }
        if (!getAiPreferences(user.id.value).canRecognizeMealPhotos) {
            _events.send(MealPhotoEvent.OpenManual)
            return
        }
        val pendingId = savedStateHandle.get<String>(MealPhotoRoute.ARG_PENDING_PHOTO_ID)
        val pending = pendingId?.let { getPendingMealPhoto(user.id.value, it) }
        if (pending != null) {
            photoIsPending = true
            _state.update {
                it.copy(photoPath = pending.filePath, form = it.form.copy(mealTime = pending.capturedAt.value))
            }
            analyze()
        } else {
            _state.update { it.copy(step = MealPhotoStep.CAMERA) }
        }
    }

    // ----- PT5 · Cámara -----

    fun onCameraPermissionResult(granted: Boolean) {
        _state.update {
            it.copy(cameraPermission = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED)
        }
    }

    fun onCaptureStarted() = _state.update { it.copy(isCapturing = true, captureFailed = false) }

    fun onPhotoCaptured(path: String) {
        _state.update {
            it.copy(
                step = MealPhotoStep.PREVIEW,
                photoPath = path,
                isCapturing = false,
                // «¿Cuándo comiste?» propuesto: el momento de la foto.
                form = it.form.copy(mealTime = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)),
            )
        }
    }

    fun onCaptureFailed() = _state.update { it.copy(isCapturing = false, captureFailed = true) }

    /** PT5 «Registrar a mano en su lugar» / PT5.M / PT7.3 / sin conexión «Registrar a mano». */
    fun onRegisterManually() {
        viewModelScope.launch {
            discardCurrentPhoto(force = true)
            _events.send(MealPhotoEvent.OpenManual)
        }
    }

    // ----- PT6 · Previsualización -----

    /** PT6 «Tomar otra vez» / PT7.3 «Tomar otra foto». */
    fun onRetake() {
        viewModelScope.launch {
            discardCurrentPhoto(force = true)
            _state.update {
                it.copy(step = MealPhotoStep.CAMERA, photoPath = null, analysis = null, failure = null, selectedAlternative = null)
            }
        }
    }

    /** PT6 «Usar esta foto» → PT6.1. */
    fun onUsePhoto() {
        viewModelScope.launch { analyze() }
    }

    private suspend fun analyze() {
        val pid = patientId ?: return
        val path = _state.value.photoPath ?: return
        _state.update { it.copy(step = MealPhotoStep.ANALYZING, failure = null) }
        val result = analyzeMealPhoto(pid, path)
        val analysis = result.getOrNull()
        if (analysis != null) {
            _state.update {
                it.copy(
                    step = MealPhotoStep.PROPOSAL,
                    analysis = analysis,
                    selectedAlternative = null,
                    adjustFood = analysis.food,
                    form = it.form.copy(portionText = gramsInput(analysis.estimatedGrams)),
                )
            }
            return
        }
        val error = result.domainErrorOrNull()
        when {
            error == DomainError.Network -> keepPhotoForLater(pid, path)
            error.isAiUnavailable() -> {
                // La IA está apagada o el paciente no la consintió: la comida se registra a mano.
                discardCurrentPhoto(force = true)
                _events.send(MealPhotoEvent.OpenManual)
            }
            error.hasCode(CODE_PHOTO_NOT_RECOGNIZED) -> {
                discardCurrentPhoto(force = true)
                _state.update { it.copy(step = MealPhotoStep.NOT_RECOGNIZED) }
            }
            else -> _state.update { it.copy(step = MealPhotoStep.ANALYSIS_FAILED, failure = error.toAnalysisFailure()) }
        }
    }

    private suspend fun keepPhotoForLater(patientId: Long, path: String) {
        if (!photoIsPending) {
            val capturedAt = LocalTimestamp.restore(_state.value.form.mealTime)
            savePendingMealPhoto(patientId, path, capturedAt)
            photoIsPending = true
        }
        _state.update { it.copy(step = MealPhotoStep.SAVED_OFFLINE) }
    }

    /** Pantalla de error de la estimación: «Volver a intentarlo». */
    fun onRetryAnalysis() {
        if (_state.value.failure == AnalysisFailure.UNUSABLE_PHOTO || _state.value.failure == AnalysisFailure.EXPIRED) {
            onRetake()
        } else {
            onUsePhoto()
        }
    }

    // ----- PT7 · Estimación propuesta -----

    /** Elige una alternativa (o vuelve a la propuesta con `null`). */
    fun onAlternativeSelected(index: Int?) {
        val analysis = _state.value.analysis ?: return
        val alternative = index?.let { analysis.selectableAlternatives.getOrNull(it) }
        _state.update {
            it.copy(
                selectedAlternative = alternative?.let { index },
                adjustFood = alternative?.food ?: analysis.food,
                form = it.form.copy(portionText = gramsInput(alternative?.grams ?: analysis.estimatedGrams)),
            )
        }
    }

    fun onPlanAnswer(inPlan: Boolean) {
        _state.update { it.copy(form = it.form.copy(inPlan = inPlan, showPlanError = false)) }
    }

    /** PT7 «Sí, es correcto»: la propuesta (o la alternativa elegida) tal cual. */
    fun onConfirmProposal() {
        val current = _state.value
        val analysis = current.analysis ?: return
        val adherence = current.form.inPlan?.let(PlanAdherence::fromAnswer)
        if (adherence == null) {
            _state.update { it.copy(form = it.form.copy(showPlanError = true)) }
            return
        }
        val timestamp = timestampOrShowAdjust() ?: return
        val alternative = current.selectedAlternative?.let { analysis.selectableAlternatives.getOrNull(it) }
        val confirmation = alternative?.let {
            val portion = PortionGrams.ofOrNull(it.grams)
            val food = it.food
            if (portion != null && food != null) PhotoConfirmation.Adjusted(food, portion) else null
        } ?: PhotoConfirmation.AsProposed
        save(analysis, confirmation, timestamp, adherence)
    }

    /** PT7 «Ajustar gramos» → PT8. */
    fun onAdjust() = _state.update { it.copy(step = MealPhotoStep.ADJUST) }

    // ----- PT8 · Confirmar o ajustar -----

    fun onPortionChange(text: String) {
        _state.update { it.copy(form = it.form.copy(portionText = text, showPortionError = false)) }
    }

    fun onMealTimeClick() = _state.update { it.copy(form = it.form.copy(showTimePicker = true)) }

    fun onMealTimeDismiss() = _state.update { it.copy(form = it.form.copy(showTimePicker = false)) }

    fun onMealTimeSelected(dateTime: LocalDateTime) {
        val value = MealFormState.offsetOf(dateTime, clock)
        val validity = LocalTimestamp.validate(value, Instant.now(clock))
        _state.update {
            it.copy(
                form = it.form.copy(
                    mealTime = value,
                    showTimePicker = false,
                    mealTimeError = validity.takeIf { v -> v != LocalTimestamp.Validity.VALID },
                ),
            )
        }
    }

    /** PT8 «¿No es este plato? Toca para cambiarlo.». */
    fun onChangeFood() {
        viewModelScope.launch { _events.send(MealPhotoEvent.PickFood) }
    }

    fun onFoodPicked(food: MealFood) {
        _state.update { it.copy(adjustFood = food, foodNotResolved = false) }
    }

    /** PT8 «Confirmar». */
    fun onConfirmAdjustment() {
        val current = _state.value
        val analysis = current.analysis ?: return
        val food = current.adjustFood ?: analysis.food
        val validation = current.form.validate(Instant.now(clock))
        _state.update { it.copy(form = validation.form) }
        if (!validation.isValid) return
        val portion = requireNotNull(validation.portion)
        val unchanged = food.referenceFoodId == analysis.food.referenceFoodId && portion.value == analysis.estimatedGrams
        val confirmation = if (unchanged) PhotoConfirmation.AsProposed else PhotoConfirmation.Adjusted(food, portion)
        save(analysis, confirmation, requireNotNull(validation.timestamp), requireNotNull(validation.planAdherence))
    }

    /** PT7.2 «Volver a intentarlo»: vuelve a la propuesta con lo elegido. */
    fun onRetrySave() = _state.update { it.copy(step = MealPhotoStep.PROPOSAL) }

    private fun save(
        analysis: MealPhotoAnalysis,
        confirmation: PhotoConfirmation,
        timestamp: LocalTimestamp,
        adherence: PlanAdherence,
    ) {
        val pid = patientId ?: return
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val meal = NewPhotoMeal(
                analysis = analysis,
                confirmation = confirmation,
                localTimestamp = timestamp,
                planAdherence = adherence,
                clientEntryId = clientEntryId,
            )
            val result = logPhotoMeal(pid, meal)
            val outcome = result.getOrNull()
            if (outcome != null) {
                discardCurrentPhoto(force = true)
                _state.update { it.copy(isSaving = false) }
                _events.send(MealPhotoEvent.Logged(outcome))
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update {
                when {
                    error.hasCode(CODE_WINDOW_EXCEEDED) -> it.copy(
                        isSaving = false,
                        step = MealPhotoStep.ADJUST,
                        form = it.form.copy(mealTimeError = LocalTimestamp.Validity.TOO_OLD),
                    )
                    error.hasCode(CODE_FOOD_NOT_RESOLVED) ->
                        it.copy(isSaving = false, step = MealPhotoStep.ADJUST, foodNotResolved = true)
                    error.hasCode(CODE_ANALYSIS_EXPIRED) || error.hasCode(CODE_ANALYSIS_NOT_FOUND) ->
                        it.copy(isSaving = false, step = MealPhotoStep.ANALYSIS_FAILED, failure = AnalysisFailure.EXPIRED)
                    else -> it.copy(isSaving = false, step = MealPhotoStep.SAVE_FAILED)
                }
            }
        }
    }

    /** El momento de la foto si sigue dentro de las 48 h; si no, abre PT8 con el error en «¿Cuándo comiste?». */
    private fun timestampOrShowAdjust(): LocalTimestamp? {
        val mealTime = _state.value.form.mealTime
        val validity = LocalTimestamp.validate(mealTime, Instant.now(clock))
        if (validity == LocalTimestamp.Validity.VALID) return LocalTimestamp.forNewEntry(mealTime, Instant.now(clock))
        _state.update { it.copy(step = MealPhotoStep.ADJUST, form = it.form.copy(mealTimeError = validity)) }
        return null
    }

    // ----- Atrás -----

    /**
     * Atrás según el frame: de la previsualización a la cámara, de PT8 a PT7, y desde PT7 se sale dejando la comida
     * «Por confirmar» (como dice la nota de PT7). En el resto se sale del flujo.
     */
    fun onBack() {
        val current = _state.value
        if (current.isSaving) return
        when (current.step) {
            MealPhotoStep.PREVIEW -> onRetake()
            MealPhotoStep.ADJUST, MealPhotoStep.SAVE_FAILED -> _state.update { it.copy(step = MealPhotoStep.PROPOSAL) }
            MealPhotoStep.PROPOSAL -> leaveUnconfirmed(current)
            else -> viewModelScope.launch {
                discardCurrentPhoto(force = false)
                _events.send(MealPhotoEvent.Exit)
            }
        }
    }

    private fun leaveUnconfirmed(current: MealPhotoUiState) {
        val pid = patientId
        val analysis = current.analysis
        if (pid == null || analysis == null) {
            viewModelScope.launch { _events.send(MealPhotoEvent.Exit) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val mealTime = current.form.mealTime
            // Fuera de la ventana no se puede guardar «Por confirmar»: la foto queda pendiente en el teléfono.
            if (LocalTimestamp.validate(mealTime, Instant.now(clock)) == LocalTimestamp.Validity.VALID) {
                val result = keepPhotoMealUnconfirmed(pid, analysis, LocalTimestamp.forNewEntry(mealTime, Instant.now(clock)), clientEntryId)
                if (result.isSuccess) discardCurrentPhoto(force = true)
            }
            _state.update { it.copy(isSaving = false) }
            _events.send(MealPhotoEvent.Exit)
        }
    }

    /**
     * Borra la foto del teléfono. Sin [force], una foto guardada como pendiente (sin conexión) se conserva para
     * analizarla después.
     */
    private suspend fun discardCurrentPhoto(force: Boolean) {
        val path = _state.value.photoPath ?: return
        if (photoIsPending && !force) return
        discardMealPhoto(path)
        photoIsPending = false
        _state.update { it.copy(photoPath = null) }
    }

    private fun DomainError?.hasCode(code: String): Boolean = when (this) {
        is DomainError.Validation -> this.code == code
        is DomainError.NotFound -> this.code == code
        is DomainError.Forbidden -> this.code == code
        is DomainError.Conflict -> this.code == code
        is DomainError.Unexpected -> this.code == code
        else -> false
    }

    /** `403` (sin consentimiento o preferencia apagada) o `503 AiFeatureDisabled` (IA apagada en el servidor). */
    private fun DomainError?.isAiUnavailable(): Boolean =
        this is DomainError.Forbidden || hasCode(CODE_AI_FEATURE_DISABLED)

    private fun DomainError?.toAnalysisFailure(): AnalysisFailure = when {
        hasCode(CODE_RATE_LIMITED) || (this as? DomainError.Unexpected)?.code == HTTP_429 -> AnalysisFailure.RATE_LIMITED
        this is DomainError.Validation -> AnalysisFailure.UNUSABLE_PHOTO
        else -> AnalysisFailure.GENERIC
    }

    /** Los gramos propuestos como texto editable («320», «62.5»). */
    private fun gramsInput(grams: Double): String =
        if (grams % 1.0 == 0.0) grams.toLong().toString() else String.format(Locale.ROOT, "%.1f", grams)

    private companion object {
        const val CODE_PHOTO_NOT_RECOGNIZED = "PhotoNotRecognized"
        const val CODE_AI_FEATURE_DISABLED = "AiFeatureDisabled"
        const val CODE_RATE_LIMITED = "AiRateLimited"
        const val HTTP_429 = "HTTP_429"
        const val CODE_WINDOW_EXCEEDED = "RetroactiveLoggingWindowExceeded"
        const val CODE_FOOD_NOT_RESOLVED = "ReferenceFoodNotResolved"
        const val CODE_ANALYSIS_EXPIRED = "MealPhotoAnalysisExpired"
        const val CODE_ANALYSIS_NOT_FOUND = "MealPhotoAnalysisNotFound"
    }
}
