package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.FieldProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementForm
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import java.time.LocalDate

/** Lo común de los pasos de la consulta: la carga de la consulta en curso (re-hidratar) y sus fallos. */
data class StepLoadState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isOffline: Boolean = false,
)

/** Un guardado que falló por algo que no es un dato del formulario (red, 5xx…): aviso con «Reintentar». */
data class SaveFailure(val isNetwork: Boolean)

/** EV-1 · Datos base (solo la primera vez; luego se editan). */
data class BaselineUiState(
    val editing: Boolean = false,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val birthDate: LocalDate? = null,
    val sex: BiologicalSex? = null,
    val heightText: String = "",
    val conditions: Set<MedicalCondition> = emptySet(),
    /** Edad calculada en vivo («Edad: 31 años (se actualiza sola)»), o `null` sin una fecha válida. */
    val ageYears: Int? = null,
    val problems: Set<BaselineProblem> = emptySet(),
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
    val showDatePicker: Boolean = false,
)

sealed interface BaselineEvent {
    /** «Guardar y salir» (o editar): vuelve a donde se abrió. */
    data object Saved : BaselineEvent

    /** «Guardar e iniciar consulta»: directo al paso en que quedó la consulta. */
    data class ConsultationStarted(val step: ConsultationStep) : BaselineEvent
}

/** EV-2 · Paso 1 Medición de hoy. */
data class MeasurementUiState(
    val load: StepLoadState = StepLoadState(),
    val baseline: BaselineSummary? = null,
    val checkIn: PatientCheckIn? = null,
    val form: MeasurementForm = MeasurementForm(),
    /** IMC en vivo con el peso escrito y la talla de los datos base. */
    val bmi: Double? = null,
    val habitsExpanded: Boolean = false,
    val biochemistryExpanded: Boolean = false,
    val problems: Map<MeasurementField, FieldProblem> = emptyMap(),
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
    val showExitDialog: Boolean = false,
)

/** EV-3 · Paso 2 Diagnóstico. */
data class DiagnosisUiState(
    val load: StepLoadState = StepLoadState(),
    val isSuggesting: Boolean = false,
    val suggestion: DiagnosisSuggestionUi? = null,
    val selected: DiagnosisCode? = null,
    /** `true` si lo elegido es la sugerencia («Usar sugerencia»); «Elegir otro» o tocar un chip lo apaga. */
    val usesSuggestion: Boolean = false,
    val showRequired: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
)

data class DiagnosisSuggestionUi(
    val code: DiagnosisCode,
    val rationale: String,
    val isFromAi: Boolean,
    val disclaimer: String,
)

/** EV-4 · Paso 3 Metas calculadas. */
data class TargetsUiState(
    val load: StepLoadState = StepLoadState(),
    val isCalculating: Boolean = false,
    val proposal: TargetProposal? = null,
    val proposalFailed: Boolean = false,
    val showParameters: Boolean = false,
    val parameters: ParametersForm? = null,
    val parametersInvalid: Boolean = false,
    val showOverride: Boolean = false,
    val override: OverrideForm = OverrideForm(),
    val overrideProblems: Set<OverrideField> = emptySet(),
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
)

/** «Cambiar parámetros». */
data class ParametersForm(
    val equation: EnergyEquation = EnergyEquation.MIFFLIN_ST_JEOR,
    val deficitKind: DeficitKind = DeficitKind.FIXED_KCAL,
    val deficit: String = "",
    val proteinPerKg: String = "",
    val fatPercent: String = "",
)

/** «Escribir mis propios valores» con la razón obligatoria (`OverrideReasonRequired`). */
data class OverrideForm(
    val energy: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val reason: String = "",
)

/** EV-5 · Paso 4 Indicaciones y publicación (+ EV-5.E). */
data class PublicationUiState(
    val load: StepLoadState = StepLoadState(),
    val restrictions: Set<PlanRestrictionCode> = emptySet(),
    val guidelines: Set<PlanGuidelineCode> = emptySet(),
    val customGuidelines: List<String> = emptyList(),
    val suggested: List<PlanGuidelineCode> = emptyList(),
    val suggestedByAi: Boolean = false,
    val customDraft: String = "",
    val showCustomField: Boolean = false,
    val customError: Boolean = false,
    val patientMessage: String = "",
    val messageTooLong: Boolean = false,
    val isPublishing: Boolean = false,
    /** EV-5.E · «No pudimos publicar el plan. Lo que escribiste no se perdió». */
    val publishFailed: Boolean = false,
    val isOffline: Boolean = false,
)
