package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import java.time.Instant

/**
 * `ConsultationResource`: the in-progress guided consultation with what each step already saved, used to
 * **rehydrate** the steps on resume ("Continue consultation"). Practitioner only.
 */
data class Consultation(
    val id: ConsultationId,
    val patientId: Long,
    val isInProgress: Boolean,
    val currentStep: ConsultationStep,
    val lastSavedAt: Instant,
    val measurement: ConsultationMeasurement?,
    val diagnosis: ConsultationDiagnosis?,
    val targets: ConsultationTargets?,
    val publicationDraft: PublicationDraft?,
    val patientCheckIn: PatientCheckIn?,
    val publishedPlanVersion: Int?,
) {
    init {
        require(patientId > 0) { "PatientId must be positive" }
    }
}

/** Saved step 1. Height, age and sex come from the baseline. */
data class ConsultationMeasurement(
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double?,
    val bodyFatPercentage: Double?,
    val bmi: Double,
    val protocolChecks: Set<ProtocolCheck>,
    val activityLevel: ActivityLevel?,
    val habits: EatingHabits?,
    val biochemistry: BiochemistryPanel?,
    val ageYears: Int,
    val sex: BiologicalSex?,
)

/** Optional eating habits: at least one value if sent. */
data class EatingHabits(val mealsPerDay: Int?, val waterLitersPerDay: Double?, val mealsOutPerWeek: Int?) {
    init {
        require(mealsPerDay != null || waterLitersPerDay != null || mealsOutPerWeek != null) {
            "Eating habits need at least one value"
        }
    }
}

/** Optional biochemistry data: at least one value if sent. */
data class BiochemistryPanel(val glucoseMgDl: Double?, val totalCholesterolMgDl: Double?, val triglyceridesMgDl: Double?) {
    init {
        require(glucoseMgDl != null || totalCholesterolMgDl != null || triglyceridesMgDl != null) {
            "A biochemistry panel needs at least one value"
        }
    }
}

/**
 * Saved step 2. `isPending`: issued in this consultation, it becomes the active diagnosis on publish; while the
 * consultation is still in progress this is normal and the step counts as done. If the measurement changes, the
 * backend discards it and the consultation comes back without a diagnosis.
 */
data class ConsultationDiagnosis(
    val code: DiagnosisCode?,
    val source: DiagnosisSource?,
    val rationale: String,
    val isPending: Boolean,
)

/** Targets (energy in kcal and macros in g). */
data class Targets(val energyKcal: Double, val proteinG: Double, val carbG: Double, val fatG: Double) {
    init {
        require(energyKcal > 0) { "Target energy must be positive" }
        require(proteinG >= 0 && carbG >= 0 && fatG >= 0) { "Macros cannot be negative" }
    }

    /** Each macro's share of the energy (4/4/9 kcal per gram), as a whole percentage. */
    val proteinPercent: Int get() = percentOf(proteinG * KCAL_PER_G_PROTEIN)
    val carbPercent: Int get() = percentOf(carbG * KCAL_PER_G_CARB)
    val fatPercent: Int get() = percentOf(fatG * KCAL_PER_G_FAT)

    private fun percentOf(kcal: Double): Int {
        val total = proteinG * KCAL_PER_G_PROTEIN + carbG * KCAL_PER_G_CARB + fatG * KCAL_PER_G_FAT
        return if (total <= 0) 0 else Math.round(kcal / total * 100).toInt()
    }

    private companion object {
        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARB = 4.0
        const val KCAL_PER_G_FAT = 9.0
    }
}

/**
 * How the targets were calculated. Practitioner-only information: it **never** reaches the patient (ethics rule 1).
 */
data class CalculationBasis(
    val equation: EnergyEquation?,
    val referenceWeightKg: Double,
    val activityFactor: Double,
    val deficitKind: DeficitKind?,
    val deficitValue: Double,
)

/** Saved step 3: the draft of version N+1. */
data class ConsultationTargets(
    val basis: CalculationBasis,
    val proposal: Targets,
    val prescribed: Targets?,
    val isOverridden: Boolean,
    val overrideReason: String?,
)

/** Step 4 draft (`publication-draft`): the choices on the publication step, so "what you wrote is not lost". */
data class PublicationDraft(
    val restrictions: Set<PlanRestrictionCode>,
    val guidelines: Set<PlanGuidelineCode>,
    val customGuidelines: List<String>,
    val patientMessage: String?,
)

/**
 * What the patient shared before the consultation (the "Before the consultation, Ana shared" card). Codes as they
 * arrive (`Good`/`Fair`/`Hard`, `Dinners`…); the questions are the patient's own text and are shown as is.
 */
data class PatientCheckIn(
    val feeling: String,
    val difficulties: List<String>,
    val questions: List<String>,
    val submittedAt: Instant,
)

/** The "AI suggestion" on the diagnosis step, or the BMI category (`source = Rule`) when AI is unavailable. */
data class DiagnosisSuggestion(
    val aiGenerationId: Long?,
    val code: DiagnosisCode,
    val rationale: String,
    val isFromAi: Boolean,
    val disclaimer: String,
)

/** `ConsultationTargetProposalResource`: the proposal and what it was calculated with ("Calculated with…"). */
data class TargetProposal(
    val basis: CalculationBasis,
    val proposal: Targets,
    val inputs: TargetInputs,
)

/** "Woman · 31 years · 168 cm · 74.2 kg · moderate activity". */
data class TargetInputs(
    val sex: BiologicalSex?,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel?,
)

/** Guidelines suggested from the diagnosis (or the fixed table, `Rule`). */
data class GuidelineSuggestions(val codes: List<PlanGuidelineCode>, val isFromAi: Boolean)

/** What the summary and the measurement step get from the baseline ("Woman · 31 years · 168 cm · hypothyroidism"). */
data class BaselineSummary(
    val sex: BiologicalSex?,
    val ageYears: Int,
    val heightCm: Double,
    val conditions: Set<MedicalCondition>,
)
