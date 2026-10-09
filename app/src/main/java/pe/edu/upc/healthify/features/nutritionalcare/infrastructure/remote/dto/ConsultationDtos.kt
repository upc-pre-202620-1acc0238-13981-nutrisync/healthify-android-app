package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/*
 * Recursos de NC-1 a NC-7 (`Healthify.Platform/NutritionalCare/Interfaces/REST/Resources`). Los pedidos no llevan
 * valores por defecto en sus campos obligatorios: con `encodeDefaults = false` un valor por defecto no se enviaría.
 */

/** `PatientBaselineResource` (NC-1). */
@Serializable
data class PatientBaselineDto(
    val patientId: Long,
    val birthDate: String,
    val ageYears: Int,
    val biologicalSex: String,
    val heightCm: Double,
    val conditions: List<String> = emptyList(),
    val updatedAt: String? = null,
    val birthDateEstimated: Boolean = false,
)

/** `RecordPatientBaselineResource` / `UpdatePatientBaselineResource` (mismo cuerpo). */
@Serializable
data class PatientBaselineRequestDto(
    val birthDate: String,
    val biologicalSex: String,
    val heightCm: Double,
    val conditions: List<String>,
)

/** `StartConsultationResource`. Sin cita = `{}`. */
@Serializable
data class StartConsultationRequestDto(val scheduledFollowUpId: Long? = null)

@Serializable
data class EatingHabitsDto(
    val mealsPerDay: Int? = null,
    val waterLitersPerDay: Double? = null,
    val mealsOutPerWeek: Int? = null,
)

@Serializable
data class BiochemistryPanelDto(
    val fastingGlucoseMgDl: Double? = null,
    val totalCholesterolMgDl: Double? = null,
    val triglyceridesMgDl: Double? = null,
)

/** `RecordConsultationMeasurementResource` (NC-3). */
@Serializable
data class MeasurementRequestDto(
    val weightKg: Double,
    val waistCm: Double? = null,
    val bodyFatPercentage: Double? = null,
    val protocolChecks: List<String>,
    val activityLevel: String,
    val habits: EatingHabitsDto? = null,
    val biochemistry: BiochemistryPanelDto? = null,
)

/** `IssueConsultationDiagnosisResource` (NC-4). */
@Serializable
data class DiagnosisRequestDto(
    val code: String,
    val source: String,
    val aiGenerationId: Long? = null,
    val rationale: String? = null,
)

/** `ConsultationTargetParametersResource`: lo que no se manda lo completa el backend con sus valores por defecto. */
@Serializable
data class TargetParametersDto(
    val equation: String,
    val deficitKind: String,
    val deficitValue: Double,
    val proteinGramsPerKg: Double,
    val fatPercentOfEnergy: Double,
)

/** `ProposeConsultationTargetsResource`: `{}` usa los parámetros por defecto. */
@Serializable
data class TargetProposalRequestDto(val parameters: TargetParametersDto? = null)

/** `PrescribeConsultationTargetsResource` (NC-5). */
@Serializable
data class PrescribeTargetsRequestDto(
    val outcome: String,
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val carbG: Double? = null,
    val fatG: Double? = null,
    val overrideReason: String? = null,
)

/** `ConsultationPublicationResource` (borrador y publicación). */
@Serializable
data class PublicationRequestDto(
    val restrictions: List<String>,
    val guidelines: List<String>,
    val customGuidelines: List<String>,
    val patientMessage: String? = null,
)

@Serializable
data class TargetsDto(
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)

@Serializable
data class CalculationBasisDto(
    val equation: String? = null,
    val referenceWeightKind: String? = null,
    val referenceWeightKg: Double = 0.0,
    val activityFactor: Double = 0.0,
    val deficitKind: String? = null,
    val deficitValue: Double = 0.0,
)

@Serializable
data class ConsultationMeasurementDto(
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val bodyFatPercentage: Double? = null,
    val bmi: Double,
    val protocolChecks: List<String> = emptyList(),
    val activityLevel: String? = null,
    val habits: EatingHabitsDto? = null,
    val biochemistry: BiochemistryPanelDto? = null,
    val ageYears: Int = 0,
    val biologicalSex: String? = null,
)

@Serializable
data class ConsultationDiagnosisDto(
    val code: String? = null,
    val source: String? = null,
    val rationale: String = "",
    val isPending: Boolean = false,
)

@Serializable
data class ConsultationTargetsDto(
    val calculationBasis: CalculationBasisDto,
    val proposal: TargetsDto,
    val prescribed: TargetsDto? = null,
    val prescriptionOutcome: String? = null,
    val overrideReason: String? = null,
)

@Serializable
data class PublicationDraftDto(
    val restrictions: List<String> = emptyList(),
    val guidelines: List<String> = emptyList(),
    val customGuidelines: List<String> = emptyList(),
    val patientMessage: String? = null,
)

@Serializable
data class CheckInQuestionDto(val text: String, val origin: String? = null)

/** `ConsultationPatientCheckInResource` (MA-4): lo que el paciente contó antes de la consulta. */
@Serializable
data class ConsultationCheckInDto(
    val feeling: String,
    val difficulties: List<String> = emptyList(),
    val questions: List<CheckInQuestionDto> = emptyList(),
    val submittedAt: String,
)

/** `ConsultationResource` (NC-2). */
@Serializable
data class ConsultationDto(
    val consultationId: Long,
    val patientId: Long,
    val state: String,
    val currentStep: String,
    val stepNumber: Int = 1,
    val lastSavedAt: String,
    val measurement: ConsultationMeasurementDto? = null,
    val diagnosis: ConsultationDiagnosisDto? = null,
    val targets: ConsultationTargetsDto? = null,
    val publicationDraft: PublicationDraftDto? = null,
    val patientCheckIn: ConsultationCheckInDto? = null,
    val publishedPlanVersion: Int? = null,
)

/** `DiagnosisSuggestionResource` (IA-6). */
@Serializable
data class DiagnosisSuggestionDto(
    val aiGenerationId: Long? = null,
    val code: String,
    val rationale: String = "",
    val source: String? = null,
    val disclaimer: String = "",
)

/** `GuidelineSuggestionsResource` (IA-7). */
@Serializable
data class GuidelineSuggestionsDto(
    val suggested: List<String> = emptyList(),
    val source: String? = null,
)

@Serializable
data class TargetInputsDto(
    val sex: String? = null,
    val ageYears: Int = 0,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val activityLevel: String? = null,
)

/** `ConsultationTargetProposalResource` (NC-5). */
@Serializable
data class TargetProposalDto(
    val calculationBasis: CalculationBasisDto,
    val proposal: TargetsDto,
    val inputsSummary: TargetInputsDto,
)
