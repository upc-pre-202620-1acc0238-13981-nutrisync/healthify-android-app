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
 * Photo logging: camera → preview → "Looking at your photo…" → proposal (or its errors) → adjustment.
 * Composes CareRelationship (whether AI photo is available) and Intake.
 *
 * - Without AI consent, without the meal photo recognition switch on, or with the AI off (`403
 *   AiConsentRequired` / `503 AiFeatureDisabled`) → log by hand.
 * - Offline when analyzing → the photo is saved on the phone to analyze it when the network returns.
 * - Nothing is stored in the diary until confirmed; confirming sends `analysisId` + `clientEntryId` (one per photo,
 *   the same on every retry).
 * - The photo is deleted from the phone on logging, on choosing to log by hand or on discarding it.
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
    private val planFitSuggester: PlanFitSuggester,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MealPhotoUiState(today = MealFormState.today(clock), form = MealFormState.now(clock)))
    val state: StateFlow<MealPhotoUiState> = _state.asStateFlow()

    private val _events = Channel<MealPhotoEvent>(Channel.BUFFERED)
    val events: Flow<MealPhotoEvent> = _events.receiveAsFlow()

    private val clientEntryId = ClientEntryId.random()
    private var patientId: Long? = null

    /** The photo was already saved as pending (it is not deleted when leaving without logging it). */
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

    // ----- Camera -----

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
                // Proposed "When did you eat?": the moment of the photo.
                form = it.form.copy(mealTime = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)),
            )
        }
    }

    fun onCaptureFailed() = _state.update { it.copy(isCapturing = false, captureFailed = true) }

    /** "Log by hand instead" from the camera, the permission dialog, the estimate error or offline. */
    fun onRegisterManually() {
        viewModelScope.launch {
            discardCurrentPhoto(force = true)
            _events.send(MealPhotoEvent.OpenManual)
        }
    }

    // ----- Preview -----

    /** "Retake" / "Take another photo". */
    fun onRetake() {
        viewModelScope.launch {
            discardCurrentPhoto(force = true)
            _state.update {
                it.copy(step = MealPhotoStep.CAMERA, photoPath = null, analysis = null, failure = null, selectedAlternative = null)
            }
        }
    }

    /** "Use this photo" → analysis. */
    fun onUsePhoto() {
        viewModelScope.launch { analyze() }
    }

    private suspend fun analyze() {
        val pid = patientId ?: return
        val path = _state.value.photoPath ?: return
        _state.update { it.copy(step = MealPhotoStep.ANALYZING, failure = null) }
        val result = analyzeMealPhoto(pid, path, _state.value.form.mealTime.toLocalDate())
        val analysis = result.getOrNull()
        if (analysis != null) {
            _state.update {
                it.copy(
                    step = MealPhotoStep.PROPOSAL,
                    analysis = analysis,
                    selectedAlternative = null,
                    adjustFood = analysis.food,
                    // A new dish brings its suggestion preselected; the answer from another photo is not carried over.
                    form = it.form.copy(portionText = gramsInput(analysis.estimatedGrams), planAnsweredByPatient = false)
                        .withPlanSuggestion(analysis.planFit),
                )
            }
            return
        }
        val error = result.domainErrorOrNull()
        when {
            error == DomainError.Network -> keepPhotoForLater(pid, path)
            error.isAiUnavailable() -> {
                // The AI is off or the patient did not consent to it: the meal is logged by hand.
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

    /** Estimate error screen: "Try again". */
    fun onRetryAnalysis() {
        if (_state.value.failure == AnalysisFailure.UNUSABLE_PHOTO || _state.value.failure == AnalysisFailure.EXPIRED) {
            onRetake()
        } else {
            onUsePhoto()
        }
    }

    // ----- Proposed estimate -----

    /** Picks an alternative (or goes back to the proposal with `null`). */
    fun onAlternativeSelected(index: Int?) {
        val analysis = _state.value.analysis ?: return
        val alternative = index?.let { analysis.selectableAlternatives.getOrNull(it) }
        planFitSuggester.cancel()
        _state.update {
            it.copy(
                selectedAlternative = alternative?.let { index },
                adjustFood = alternative?.food ?: analysis.food,
                form = it.form.copy(portionText = gramsInput(alternative?.grams ?: analysis.estimatedGrams))
                    .withPlanSuggestion(if (alternative != null) alternative.planFit else analysis.planFit),
            )
        }
    }

    fun onPlanAnswer(inPlan: Boolean) {
        _state.update { it.copy(form = it.form.answerPlan(inPlan)) }
    }

    /** "Yes, that's right": the proposal (or the picked alternative) as is. */
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

    /** "Adjust grams" → adjustment. */
    fun onAdjust() = _state.update { it.copy(step = MealPhotoStep.ADJUST) }

    // ----- Confirm or adjust -----

    fun onPortionChange(text: String) {
        _state.update { it.copy(form = it.form.copy(portionText = text, showPortionError = false)) }
        requestPlanFit()
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
        requestPlanFit()
    }

    /** "Not this dish? Tap to change it.". */
    fun onChangeFood() {
        viewModelScope.launch { _events.send(MealPhotoEvent.PickFood) }
    }

    fun onFoodPicked(food: MealFood) {
        _state.update { it.copy(adjustFood = food, foodNotResolved = false) }
        requestPlanFit()
    }

    /** The suggestion follows the dish, portion and day the patient is picking. */
    private fun requestPlanFit() {
        val pid = patientId ?: return
        val current = _state.value
        planFitSuggester.request(viewModelScope, pid, current.adjustFood ?: current.analysis?.food, current.form) { fit ->
            _state.update { it.copy(form = it.form.withPlanSuggestion(fit)) }
        }
    }

    /** "Confirm". */
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

    /** "Try again" after a failed log: back to the proposal with what was picked. */
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

    /** The photo's moment if still within 48 h; otherwise the adjustment opens with a "When did you eat?" error. */
    private fun timestampOrShowAdjust(): LocalTimestamp? {
        val mealTime = _state.value.form.mealTime
        val validity = LocalTimestamp.validate(mealTime, Instant.now(clock))
        if (validity == LocalTimestamp.Validity.VALID) return LocalTimestamp.forNewEntry(mealTime, Instant.now(clock))
        _state.update { it.copy(step = MealPhotoStep.ADJUST, form = it.form.copy(mealTimeError = validity)) }
        return null
    }

    // ----- Back -----

    /**
     * Back as in the design: from the preview to the camera, from the adjustment to the estimate, and from the estimate
     * it leaves the meal "To confirm" (as the estimate's note says). Anywhere else it leaves the flow.
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
            // Outside the window "To confirm" cannot be saved: the photo stays pending on the phone.
            if (LocalTimestamp.validate(mealTime, Instant.now(clock)) == LocalTimestamp.Validity.VALID) {
                val result = keepPhotoMealUnconfirmed(pid, analysis, LocalTimestamp.forNewEntry(mealTime, Instant.now(clock)), clientEntryId)
                if (result.isSuccess) discardCurrentPhoto(force = true)
            }
            _state.update { it.copy(isSaving = false) }
            _events.send(MealPhotoEvent.Exit)
        }
    }

    /**
     * Deletes the photo from the phone. Without [force], a photo saved as pending (offline) is kept to
     * analyze it later.
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

    /** `403` (no consent or preference off) or `503 AiFeatureDisabled` (AI off on the server). */
    private fun DomainError?.isAiUnavailable(): Boolean =
        this is DomainError.Forbidden || hasCode(CODE_AI_FEATURE_DISABLED)

    private fun DomainError?.toAnalysisFailure(): AnalysisFailure = when {
        hasCode(CODE_RATE_LIMITED) || (this as? DomainError.Unexpected)?.code == HTTP_429 -> AnalysisFailure.RATE_LIMITED
        this is DomainError.Validation -> AnalysisFailure.UNUSABLE_PHOTO
        else -> AnalysisFailure.GENERIC
    }

    /** The proposed grams as editable text ("320", "62.5"). */
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
