package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import java.time.Instant
import java.time.LocalDate

/**
 * `PatientSummaryResource`: the Summary tab. `baseline = null` means there is no baseline yet;
 * a non-null `consultationInProgress` means a consultation is in progress.
 */
data class PatientSummary(
    val patientId: Long,
    val fullName: String?,
    val linkedSince: Instant?,
    val isLinkActive: Boolean,
    val activePlanVersion: Int?,
    val baseline: BaselineSummary?,
    val nextFollowUp: SummaryFollowUp?,
    val consultationInProgress: ConsultationInProgress?,
    val sinceLastConsultation: SinceLastConsultation,
) {
    val hasBaseline: Boolean get() = baseline != null

    /** "New": no baseline or no published plan (same rule as the caseload). */
    val isNew: Boolean get() = baseline == null || activePlanVersion == null
}

/** The summary's "Next consultation". */
data class SummaryFollowUp(val followUpId: Long, val scheduledFor: Instant)

/** "Consultation in progress · Step 1 of 4 · Today's measurement · saved today". */
data class ConsultationInProgress(val id: ConsultationId, val step: ConsultationStep, val lastSavedAt: Instant)

/**
 * "SINCE THE LAST CONSULTATION": slope of the self-weigh-in trend (never a single day's weight, ethics rule 6) and days
 * within targets over calendar days (a day without logs is not a miss: it just does not add up).
 */
data class SinceLastConsultation(
    val fromDate: LocalDate,
    val weightSlopeKgPerWeek: Double?,
    val trendWeeks: Int,
    val compliance: ComplianceRatio?,
)

data class ComplianceRatio(val met: Int, val total: Int) {
    init {
        require(met >= 0 && total >= 0 && met <= total) { "Met days cannot exceed the total" }
    }
}

/**
 * `PractitionerPatientRecordResource`: the Record tab. It includes the **active diagnosis**, which is
 * practitioner-only. Any section can be missing if its context did not respond.
 */
data class PractitionerPatientRecord(
    val clinicalWeight: ClinicalWeightSummary?,
    val bmi: RecordBmi?,
    val activeTargets: RecordActiveTargets?,
    val compliance: RecordComplianceWindow?,
    val activeDiagnosis: RecordDiagnosis?,
    val referrals: List<ReferralSummary>,
    val evaluations: List<RecordEvaluation>,
)

data class ClinicalWeightSummary(val latestKg: Double, val latestDate: Instant, val deltaKgSinceFirst: Double, val firstDate: Instant)

data class RecordBmi(val value: Double, val category: DiagnosisCode?)

data class RecordActiveTargets(val planVersion: Int, val validFrom: Instant, val energyKcal: Double)

data class RecordComplianceWindow(val from: LocalDate, val to: LocalDate, val ratio: ComplianceRatio)

/** "Overweight class I · Mar 3, 2026 · practitioner only". `code = null`: diagnosis older than the closed list. */
data class RecordDiagnosis(val code: DiagnosisCode?, val statement: String, val issuedAt: Instant)

/** Referral: specialty and reason are the practitioner's text; they are not translated. */
data class ReferralSummary(
    val id: Long,
    val specialty: String,
    val issuedAt: Instant,
    val isOpen: Boolean,
    val reason: String = "",
    val closedAt: Instant? = null,
)

/** "Sep 3, 2026 · 74.2 kg · BMI 26.3 · waist 88 cm" / "… · first assessment". */
data class RecordEvaluation(
    val assessmentId: Long,
    val takenAt: Instant,
    val weightKg: Double,
    val bmi: Double,
    val waistCm: Double?,
    val isFirst: Boolean,
)

/**
 * Detail of a record assessment (`GET /nutritional-assessments/{id}`, only for the practitioner who recorded
 * it): the assessment's latest measurement, with the baseline at that time.
 */
data class EvaluationDetail(
    val assessmentId: Long,
    val takenAt: Instant,
    val measurement: ConsultationMeasurement,
    val bmiCategory: DiagnosisCode?,
)

/** A published plan version ("PLAN HISTORY" on the Plan tab). */
data class NutritionPlanVersion(
    val version: Int,
    val isActive: Boolean,
    val publishedAt: Instant,
    val targets: Targets,
    val guidelines: List<PlanGuidelineItem>,
    val restrictions: List<PlanRestrictionItem>,
    val patientMessage: String?,
)

/** A version guideline: from the catalog (translated) or custom/legacy (as is). */
sealed interface PlanGuidelineItem {
    data class Catalog(val code: PlanGuidelineCode) : PlanGuidelineItem
    data class Custom(val text: String) : PlanGuidelineItem
}

/** A version restriction: from the closed list (translated) or text that predates the list (as is). */
sealed interface PlanRestrictionItem {
    data class Catalog(val code: PlanRestrictionCode) : PlanRestrictionItem
    data class Legacy(val text: String) : PlanRestrictionItem
}

/** Published versions, the current one first. */
data class PlanHistory(val versions: List<NutritionPlanVersion>) {
    val active: NutritionPlanVersion? get() = versions.firstOrNull { it.isActive }

    companion object {
        /** Sorts from newest to oldest. */
        fun of(versions: List<NutritionPlanVersion>): PlanHistory =
            PlanHistory(versions.sortedByDescending { it.version })
    }
}
