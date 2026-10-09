package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `PatientBaselineSummaryResource` (RM-2). */
@Serializable
data class BaselineSummaryDto(
    val biologicalSex: String? = null,
    val ageYears: Int = 0,
    val heightCm: Double = 0.0,
    val conditions: List<String> = emptyList(),
)

@Serializable
data class SummaryFollowUpDto(val followUpId: Long, val scheduledFor: String)

@Serializable
data class SummaryConsultationInProgressDto(
    val consultationId: Long,
    val stepNumber: Int = 1,
    val stepName: String? = null,
    val lastSavedAt: String,
)

@Serializable
data class ComplianceRatioDto(val met: Int, val total: Int)

@Serializable
data class SinceLastConsultationDto(
    val fromDate: String,
    val weightSlopeKgPerWeek: Double? = null,
    val trendWeeks: Int = 0,
    val compliance: ComplianceRatioDto? = null,
)

/** `PatientSummaryResource` (RM-2). */
@Serializable
data class PatientSummaryDto(
    val patientId: Long,
    val fullName: String? = null,
    val linkedSince: String? = null,
    val linkStatus: String? = null,
    val activePlanVersion: Int? = null,
    val baseline: BaselineSummaryDto? = null,
    val nextFollowUp: SummaryFollowUpDto? = null,
    val consultationInProgress: SummaryConsultationInProgressDto? = null,
    val sinceLastConsultation: SinceLastConsultationDto,
)

@Serializable
data class ActiveDiagnosisDto(val code: String? = null, val statement: String = "", val issuedAt: String)

@Serializable
data class EvaluationDto(
    val assessmentId: Long,
    val takenAt: String,
    val weightKg: Double,
    val bmi: Double,
    val waistCm: Double? = null,
    val isFirst: Boolean = false,
)

@Serializable
data class ClinicalWeightSummaryDto(
    val latestKg: Double,
    val latestDate: String,
    val deltaKgSinceFirst: Double,
    val firstDate: String,
)

@Serializable
data class BmiDto(val value: Double, val category: String? = null)

@Serializable
data class PractitionerComplianceDto(val from: String, val to: String, val met: Int, val total: Int)

@Serializable
data class InterventionDto(val planVersion: Int, val validFrom: String, val energyKcal: Double)

@Serializable
data class ReferralSummaryDto(
    val referralId: Long,
    val specialty: String,
    val issuedAt: String,
    val status: String = "Open",
)

@Serializable
data class PractitionerFollowUpSectionDto(val referrals: List<ReferralSummaryDto> = emptyList())

/** `PractitionerPatientRecordResource` (RM-4): `PatientRecordResource` + lo del profesional. */
@Serializable
data class PractitionerRecordDto(
    val patientId: Long,
    val intervention: InterventionDto? = null,
    val followUp: PractitionerFollowUpSectionDto? = null,
    val activeDiagnosis: ActiveDiagnosisDto? = null,
    val evaluations: List<EvaluationDto> = emptyList(),
    val clinicalWeight: ClinicalWeightSummaryDto? = null,
    val bmi: BmiDto? = null,
    val compliance: PractitionerComplianceDto? = null,
)

@Serializable
data class GuidelineItemDto(val code: String? = null, val custom: String? = null)

/** `NutritionPlanResource` (PAC-4). */
@Serializable
data class NutritionPlanDto(
    val planId: Long,
    val version: Int,
    val proposal: TargetsDto,
    val prescribed: TargetsDto? = null,
    val guidelines: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val isActive: Boolean = false,
    val publishedAt: String? = null,
    val guidelineItems: List<GuidelineItemDto>? = null,
    val legacyRestrictions: List<String>? = null,
    val patientMessage: String? = null,
)
