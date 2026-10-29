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

/** What the consultation steps share: loading the in-progress consultation (rehydrate) and its failures. */
data class StepLoadState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isOffline: Boolean = false,
)

/** A save that failed for a reason other than form data (network, 5xx…): notice with "Retry". */
data class SaveFailure(val isNetwork: Boolean)

/** Baseline (only the first time; edited afterwards). */
data class BaselineUiState(
    val editing: Boolean = false,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val birthDate: LocalDate? = null,
    val sex: BiologicalSex? = null,
    val heightText: String = "",
    val conditions: Set<MedicalCondition> = emptySet(),
    /** Age calculated live ("Age: 31 years (updates itself)"), or `null` without a valid date. */
    val ageYears: Int? = null,
    val problems: Set<BaselineProblem> = emptySet(),
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
    val showDatePicker: Boolean = false,
)

sealed interface BaselineEvent {
    /** "Save and exit" (or edit): back to where it was opened. */
    data object Saved : BaselineEvent

    /** "Save and start consultation": straight to the step the consultation is on. */
    data class ConsultationStarted(val step: ConsultationStep) : BaselineEvent
}

/** Step 1, today's measurement. */
data class MeasurementUiState(
    val load: StepLoadState = StepLoadState(),
    val baseline: BaselineSummary? = null,
    val checkIn: PatientCheckIn? = null,
    val form: MeasurementForm = MeasurementForm(),
    /** Live BMI from the typed weight and the baseline height. */
    val bmi: Double? = null,
    val habitsExpanded: Boolean = false,
    val biochemistryExpanded: Boolean = false,
    val problems: Map<MeasurementField, FieldProblem> = emptyMap(),
    val isSaving: Boolean = false,
    val saveFailure: SaveFailure? = null,
    val showExitDialog: Boolean = false,
    /** "Delete consultation" (trash can): confirmation dialog, deletion in progress and failure with "Retry". */
    val showDiscardDialog: Boolean = false,
    val isDiscarding: Boolean = false,
    val discardFailure: SaveFailure? = null,
)

/** Step 2, diagnosis. */
data class DiagnosisUiState(
    val load: StepLoadState = StepLoadState(),
    val isSuggesting: Boolean = false,
    val suggestion: DiagnosisSuggestionUi? = null,
    val selected: DiagnosisCode? = null,
    /** `true` if the choice is the suggestion ("Use suggestion"); "Choose another" or tapping a chip turns it off. */
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

/** Step 3, calculated targets. */
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

/** "Change parameters". */
data class ParametersForm(
    val equation: EnergyEquation = EnergyEquation.MIFFLIN_ST_JEOR,
    val deficitKind: DeficitKind = DeficitKind.FIXED_KCAL,
    val deficit: String = "",
    val proteinPerKg: String = "",
    val fatPercent: String = "",
)

/** "Enter my own values" with the required reason (`OverrideReasonRequired`). */
data class OverrideForm(
    val energy: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val reason: String = "",
)

/** Step 4, guidelines and publication (including the publish error). */
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
    /** "We could not publish the plan. What you wrote was not lost". */
    val publishFailed: Boolean = false,
    val isOffline: Boolean = false,
)
