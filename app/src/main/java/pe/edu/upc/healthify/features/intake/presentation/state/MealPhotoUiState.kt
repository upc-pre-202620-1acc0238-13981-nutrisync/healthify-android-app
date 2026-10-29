package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import java.time.LocalDate

/** Which step of the photo flow the patient is on. */
enum class MealPhotoStep {
    /** Checking whether AI photo is available (consent + preference). */
    RESOLVING,

    /** Camera (and the permission dialog if the permission is denied). */
    CAMERA,

    /** Photo preview. */
    PREVIEW,

    /** "Looking at your photo…". */
    ANALYZING,

    /** Offline: the photo was saved to be analyzed later. */
    SAVED_OFFLINE,

    /** Proposed estimate. */
    PROPOSAL,

    /** Confirm or adjust. */
    ADJUST,

    /** Could not log. */
    SAVE_FAILED,

    /** We could not estimate. */
    NOT_RECOGNIZED,

    /** The AI could not look at the photo right now (quota, invalid photo, server). */
    ANALYSIS_FAILED,
}

enum class CameraPermissionStatus { UNKNOWN, GRANTED, DENIED }

/** Why there was no estimate (text for the error screen). */
enum class AnalysisFailure { GENERIC, RATE_LIMITED, UNUSABLE_PHOTO, EXPIRED }

/**
 * Photo logging, from camera to adjustment. The AI proposal is never stored as intake until the patient confirms.
 *
 * @param selectedAlternative index in `analysis.selectableAlternatives` if another dish was picked on the estimate.
 * @param adjustFood the dish being adjusted (the proposed one, an alternative or one picked from the catalog).
 * @param foodNotResolved the backend did not resolve the picked dish (`422 ReferenceFoodNotResolved`).
 */
data class MealPhotoUiState(
    val today: LocalDate,
    /** The day the account was created: the day picker does not offer earlier days. */
    val earliestDay: LocalDate? = null,
    val form: MealFormState,
    val step: MealPhotoStep = MealPhotoStep.RESOLVING,
    val cameraPermission: CameraPermissionStatus = CameraPermissionStatus.UNKNOWN,
    val photoPath: String? = null,
    val analysis: MealPhotoAnalysis? = null,
    val selectedAlternative: Int? = null,
    val adjustFood: MealFood? = null,
    val foodNotResolved: Boolean = false,
    val failure: AnalysisFailure? = null,
    val isSaving: Boolean = false,
    val isCapturing: Boolean = false,
    val captureFailed: Boolean = false,
    val isOffline: Boolean = false,
) {
    /** The dish that "Yes, that's right" would log (the picked alternative or the proposed one). */
    val chosenFood: MealFood?
        get() = selectedAlternative?.let { analysis?.selectableAlternatives?.getOrNull(it)?.food } ?: analysis?.food

    val chosenGrams: Double?
        get() = selectedAlternative?.let { analysis?.selectableAlternatives?.getOrNull(it)?.grams } ?: analysis?.estimatedGrams
}

sealed interface MealPhotoEvent {
    /** "Log by hand" (or AI photo is not available): manual logging instead of this flow. */
    data object OpenManual : MealPhotoEvent

    /** "Not this dish? Tap to change it." → food search in pick mode. */
    data object PickFood : MealPhotoEvent

    /** Meal logged or queued → Diary with Snackbar. */
    data class Logged(val outcome: MealLogOutcome) : MealPhotoEvent

    /** Leave the flow (the photo was already deleted or stayed pending). */
    data object Exit : MealPhotoEvent
}
