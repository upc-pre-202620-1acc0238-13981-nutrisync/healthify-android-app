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
 * `ConsultationResource` (NC-2): la consulta guiada en curso con lo que cada paso ya guardó, para **re-hidratar**
 * EV-2 a EV-5 al reanudar (PAC-1.C «Continuar consulta»). Solo del profesional.
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

/** Paso 1 guardado (NC-3). Talla, edad y sexo vienen de los datos base. */
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

/** Hábitos alimentarios opcionales (NC-3): al menos un valor si se envían. */
data class EatingHabits(val mealsPerDay: Int?, val waterLitersPerDay: Double?, val mealsOutPerWeek: Int?) {
    init {
        require(mealsPerDay != null || waterLitersPerDay != null || mealsOutPerWeek != null) {
            "Eating habits need at least one value"
        }
    }
}

/** Datos bioquímicos opcionales (NC-3): al menos un valor si se envían. */
data class BiochemistryPanel(val glucoseMgDl: Double?, val totalCholesterolMgDl: Double?, val triglyceridesMgDl: Double?) {
    init {
        require(glucoseMgDl != null || totalCholesterolMgDl != null || triglyceridesMgDl != null) {
            "A biochemistry panel needs at least one value"
        }
    }
}

/**
 * Paso 2 guardado (NC-4). `isPending` (NC-7): emitido en esta consulta, pasa a ser el diagnóstico activo al publicar;
 * mientras la consulta sigue en curso es lo normal y el paso cuenta como hecho. Si la medición cambia, el backend lo
 * descarta y la consulta llega sin diagnóstico.
 */
data class ConsultationDiagnosis(
    val code: DiagnosisCode?,
    val source: DiagnosisSource?,
    val rationale: String,
    val isPending: Boolean,
)

/** Metas (energía en kcal y macros en g). */
data class Targets(val energyKcal: Double, val proteinG: Double, val carbG: Double, val fatG: Double) {
    init {
        require(energyKcal > 0) { "Target energy must be positive" }
        require(proteinG >= 0 && carbG >= 0 && fatG >= 0) { "Macros cannot be negative" }
    }

    /** Parte de la energía de cada macro (4/4/9 kcal por gramo), en porcentaje entero. */
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
 * Base de cálculo de las metas (F9). Es información del profesional: **nunca** sale hacia el paciente (regla ética 1).
 */
data class CalculationBasis(
    val equation: EnergyEquation?,
    val referenceWeightKg: Double,
    val activityFactor: Double,
    val deficitKind: DeficitKind?,
    val deficitValue: Double,
)

/** Paso 3 guardado (NC-5): el borrador de la versión N+1. */
data class ConsultationTargets(
    val basis: CalculationBasis,
    val proposal: Targets,
    val prescribed: Targets?,
    val isOverridden: Boolean,
    val overrideReason: String?,
)

/** Borrador del paso 4 (`publication-draft`): lo elegido en EV-5, para que «lo que escribiste no se pierda». */
data class PublicationDraft(
    val restrictions: Set<PlanRestrictionCode>,
    val guidelines: Set<PlanGuidelineCode>,
    val customGuidelines: List<String>,
    val patientMessage: String?,
)

/**
 * Lo que el paciente contó antes de la consulta (MA-4, EV-2 «Antes de la consulta, Ana contó»). Códigos tal como
 * llegan (`Good`/`Fair`/`Hard`, `Dinners`…); las preguntas son texto del paciente y se muestran tal cual.
 */
data class PatientCheckIn(
    val feeling: String,
    val difficulties: List<String>,
    val questions: List<String>,
    val submittedAt: Instant,
)

/** IA-6 · «Sugerencia de IA» de EV-3, o la categoría del IMC (`source = Rule`) si la IA no está disponible. */
data class DiagnosisSuggestion(
    val aiGenerationId: Long?,
    val code: DiagnosisCode,
    val rationale: String,
    val isFromAi: Boolean,
    val disclaimer: String,
)

/** NC-5 · `ConsultationTargetProposalResource`: la propuesta y con qué se calculó («Calculado con…»). */
data class TargetProposal(
    val basis: CalculationBasis,
    val proposal: Targets,
    val inputs: TargetInputs,
)

/** «Mujer · 31 años · 168 cm · 74.2 kg · actividad moderada». */
data class TargetInputs(
    val sex: BiologicalSex?,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel?,
)

/** IA-7 · indicaciones sugeridas según el diagnóstico (o la tabla fija, `Rule`). */
data class GuidelineSuggestions(val codes: List<PlanGuidelineCode>, val isFromAi: Boolean)

/** Lo que reciben los datos base en el resumen y en EV-2 («Mujer · 31 años · 168 cm · hipotiroidismo»). */
data class BaselineSummary(
    val sex: BiologicalSex?,
    val ageYears: Int,
    val heightCm: Double,
    val conditions: Set<MedicalCondition>,
)
