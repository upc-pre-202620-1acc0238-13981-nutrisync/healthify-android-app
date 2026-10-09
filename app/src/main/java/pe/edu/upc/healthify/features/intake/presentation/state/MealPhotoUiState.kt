package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import java.time.LocalDate

/** En qué frame del flujo por foto está el paciente. */
enum class MealPhotoStep {
    /** Revisando si la foto con IA está disponible (consentimiento + preferencia). */
    RESOLVING,

    /** PT5 Cámara (+ PT5.M si el permiso está denegado). */
    CAMERA,

    /** PT6 Previsualización. */
    PREVIEW,

    /** PT6.1 «Viendo tu foto…». */
    ANALYZING,

    /** Sin conexión: la foto quedó guardada para analizarla después. */
    SAVED_OFFLINE,

    /** PT7 Estimación propuesta. */
    PROPOSAL,

    /** PT8 Confirmar o ajustar. */
    ADJUST,

    /** PT7.2 No se pudo registrar. */
    SAVE_FAILED,

    /** PT7.3 No pudimos estimar. */
    NOT_RECOGNIZED,

    /** La IA no pudo ver la foto ahora (cuota, foto inválida, servidor). */
    ANALYSIS_FAILED,
}

enum class CameraPermissionStatus { UNKNOWN, GRANTED, DENIED }

/** Por qué no hubo estimación (texto de la pantalla de error). */
enum class AnalysisFailure { GENERIC, RATE_LIMITED, UNUSABLE_PHOTO, EXPIRED }

/**
 * PT5 → PT8 (IN-7). La propuesta de la IA nunca se guarda como ingesta hasta que el paciente confirma.
 *
 * @param selectedAlternative índice en `analysis.selectableAlternatives` si eligió otro plato en PT7.
 * @param adjustFood el plato de PT8 (el propuesto, una alternativa o uno elegido del catálogo).
 * @param foodNotResolved el backend no resolvió el plato elegido (`422 ReferenceFoodNotResolved`).
 */
data class MealPhotoUiState(
    val today: LocalDate,
    /** Día en que se creó la cuenta: el selector del día no ofrece días anteriores. */
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
    /** El plato que se registraría con «Sí, es correcto» (la alternativa elegida o el propuesto). */
    val chosenFood: MealFood?
        get() = selectedAlternative?.let { analysis?.selectableAlternatives?.getOrNull(it)?.food } ?: analysis?.food

    val chosenGrams: Double?
        get() = selectedAlternative?.let { analysis?.selectableAlternatives?.getOrNull(it)?.grams } ?: analysis?.estimatedGrams
}

sealed interface MealPhotoEvent {
    /** «Registrar a mano» (o la foto con IA no está disponible): PT9 en lugar de este flujo. */
    data object OpenManual : MealPhotoEvent

    /** PT8 «¿No es este plato? Toca para cambiarlo.» → PT9 en modo elegir. */
    data object PickFood : MealPhotoEvent

    /** Comida registrada o encolada → Diario con Snackbar (PT14.1). */
    data class Logged(val outcome: MealLogOutcome) : MealPhotoEvent

    /** Salir del flujo (la foto ya se borró o quedó pendiente). */
    data object Exit : MealPhotoEvent
}
