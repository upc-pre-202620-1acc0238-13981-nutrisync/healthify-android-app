package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import java.time.Instant
import java.time.LocalDate

/**
 * `PatientSummaryResource` (RM-2): la pestaña Resumen (PAC-1). `baseline = null` es PAC-0 (aún sin datos base);
 * `consultationInProgress` no nulo es PAC-1.C.
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

    /** «Nueva»: sin datos base o sin plan publicado (mismo criterio que la cartera, RM-1). */
    val isNew: Boolean get() = baseline == null || activePlanVersion == null
}

/** «Próxima consulta» del resumen. */
data class SummaryFollowUp(val followUpId: Long, val scheduledFor: Instant)

/** PAC-1.C · «Consulta en curso · Paso 1 de 4 · Medición de hoy · se guardó hoy». */
data class ConsultationInProgress(val id: ConsultationId, val step: ConsultationStep, val lastSavedAt: Instant)

/**
 * «DESDE LA ÚLTIMA CONSULTA»: pendiente de la tendencia del autopesaje (nunca el peso de un día, regla ética 6) y días
 * dentro de metas sobre días del calendario (un día sin registro no es incumplimiento: solo no suma).
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
 * `PractitionerPatientRecordResource` (RM-4): la pestaña Expediente (PAC-3). Incluye el **diagnóstico activo**, que es
 * solo del profesional. Cada sección puede faltar si su contexto no respondió.
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

/** «Sobrepeso grado I · 3 mar. 2026 · solo profesional». `code = null`: diagnóstico anterior a la lista cerrada. */
data class RecordDiagnosis(val code: DiagnosisCode?, val statement: String, val issuedAt: Instant)

/** Derivación (MA, F25): especialidad y motivo son texto del profesional; no se traducen. */
data class ReferralSummary(val id: Long, val specialty: String, val issuedAt: Instant, val isOpen: Boolean)

/** «3 sept. 2026 · 74.2 kg · IMC 26.3 · cintura 88 cm» / «… · primera evaluación». */
data class RecordEvaluation(
    val assessmentId: Long,
    val takenAt: Instant,
    val weightKg: Double,
    val bmi: Double,
    val waistCm: Double?,
    val isFirst: Boolean,
)

/** Una versión publicada del plan (PAC-4 «HISTORIAL DEL PLAN»). */
data class NutritionPlanVersion(
    val version: Int,
    val isActive: Boolean,
    val publishedAt: Instant,
    val targets: Targets,
    val guidelines: List<PlanGuidelineItem>,
    val restrictions: List<PlanRestrictionItem>,
    val patientMessage: String?,
)

/** Indicación de una versión: del catálogo (se traduce) o propia/legada (tal cual). */
sealed interface PlanGuidelineItem {
    data class Catalog(val code: PlanGuidelineCode) : PlanGuidelineItem
    data class Custom(val text: String) : PlanGuidelineItem
}

/** Restricción de una versión: de la lista cerrada (se traduce) o un texto anterior a la lista (tal cual). */
sealed interface PlanRestrictionItem {
    data class Catalog(val code: PlanRestrictionCode) : PlanRestrictionItem
    data class Legacy(val text: String) : PlanRestrictionItem
}

/** PAC-4: versiones publicadas, la vigente primero. */
data class PlanHistory(val versions: List<NutritionPlanVersion>) {
    val active: NutritionPlanVersion? get() = versions.firstOrNull { it.isActive }

    companion object {
        /** Ordena de la más nueva a la más vieja. */
        fun of(versions: List<NutritionPlanVersion>): PlanHistory =
            PlanHistory(versions.sortedByDescending { it.version })
    }
}
