package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ClinicalRange
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.CustomGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DecimalText
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.NumberInput
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.OverrideReason
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck

/** Step 1 · `PUT /consultations/{id}/measurement`. At least one protocol checkbox and the activity. */
data class NewMeasurement(
    val weightKg: Double,
    val waistCm: Double?,
    val bodyFatPercentage: Double?,
    val protocolChecks: Set<ProtocolCheck>,
    val activityLevel: ActivityLevel,
    val habits: EatingHabits?,
    val biochemistry: BiochemistryPanel?,
) {
    init {
        require(ClinicalRange.WEIGHT_KG.contains(weightKg)) { "Implausible weight" }
        require(waistCm == null || ClinicalRange.WAIST_CM.contains(waistCm)) { "Implausible waist" }
        require(bodyFatPercentage == null || ClinicalRange.BODY_FAT_PERCENT.contains(bodyFatPercentage)) {
            "Implausible body fat"
        }
        require(protocolChecks.isNotEmpty()) { "At least one protocol check must be ticked" }
    }

    /** `true` if it matches what step 1 already saved: not resent (repeating it forces redoing step 2). */
    fun sameAs(saved: ConsultationMeasurement): Boolean =
        weightKg == saved.weightKg && waistCm == saved.waistCm && bodyFatPercentage == saved.bodyFatPercentage &&
            protocolChecks == saved.protocolChecks && activityLevel == saved.activityLevel && habits == saved.habits &&
            biochemistry == saved.biochemistry
}

/** Measurement step fields the phone validates. */
enum class MeasurementField {
    WEIGHT, WAIST, BODY_FAT, PROTOCOL, ACTIVITY, MEALS_PER_DAY, WATER, MEALS_OUT, GLUCOSE, CHOLESTEROL, TRIGLYCERIDES
}

/** Why a measurement field is invalid: empty while required, or outside the backend range. */
enum class FieldProblem { REQUIRED, OUT_OF_RANGE }

/** The text of each measurement field exactly as the practitioner typed it. */
data class MeasurementForm(
    val weight: String = "",
    val waist: String = "",
    val bodyFat: String = "",
    val protocolChecks: Set<ProtocolCheck> = emptySet(),
    val activityLevel: ActivityLevel? = null,
    val mealsPerDay: String = "",
    val waterLiters: String = "",
    val mealsOut: String = "",
    val glucose: String = "",
    val cholesterol: String = "",
    val triglycerides: String = "",
)

sealed interface MeasurementValidation {
    data class Valid(val measurement: NewMeasurement) : MeasurementValidation
    data class Invalid(val problems: Map<MeasurementField, FieldProblem>) : MeasurementValidation
}

/** Checks the backend ranges (`ImplausibleMeasurement`, `ProtocolChecklistEmpty`, `InvalidEatingHabits`…). */
fun MeasurementForm.validate(): MeasurementValidation {
    val problems = mutableMapOf<MeasurementField, FieldProblem>()

    fun read(range: ClinicalRange, text: String, field: MeasurementField, required: Boolean = false): Double? =
        when (val input = range.parse(text)) {
            is NumberInput.Valid -> input.value
            NumberInput.Empty -> null.also { if (required) problems[field] = FieldProblem.REQUIRED }
            NumberInput.OutOfRange -> null.also { problems[field] = FieldProblem.OUT_OF_RANGE }
        }

    val weight = read(ClinicalRange.WEIGHT_KG, weight, MeasurementField.WEIGHT, required = true)
    val waist = read(ClinicalRange.WAIST_CM, waist, MeasurementField.WAIST)
    val bodyFat = read(ClinicalRange.BODY_FAT_PERCENT, bodyFat, MeasurementField.BODY_FAT)
    val meals = read(ClinicalRange.MEALS_PER_DAY, mealsPerDay, MeasurementField.MEALS_PER_DAY)
    val water = read(ClinicalRange.WATER_LITERS_PER_DAY, waterLiters, MeasurementField.WATER)
    val mealsOut = read(ClinicalRange.MEALS_OUT_PER_WEEK, mealsOut, MeasurementField.MEALS_OUT)
    val glucose = read(ClinicalRange.GLUCOSE_MG_DL, glucose, MeasurementField.GLUCOSE)
    val cholesterol = read(ClinicalRange.TOTAL_CHOLESTEROL_MG_DL, cholesterol, MeasurementField.CHOLESTEROL)
    val triglycerides = read(ClinicalRange.TRIGLYCERIDES_MG_DL, triglycerides, MeasurementField.TRIGLYCERIDES)
    if (protocolChecks.isEmpty()) problems[MeasurementField.PROTOCOL] = FieldProblem.REQUIRED
    if (activityLevel == null) problems[MeasurementField.ACTIVITY] = FieldProblem.REQUIRED

    if (problems.isNotEmpty() || weight == null || activityLevel == null) return MeasurementValidation.Invalid(problems)
    val habits = if (meals != null || water != null || mealsOut != null) {
        EatingHabits(meals?.toInt(), water, mealsOut?.toInt())
    } else {
        null
    }
    val biochemistry = if (glucose != null || cholesterol != null || triglycerides != null) {
        BiochemistryPanel(glucose, cholesterol, triglycerides)
    } else {
        null
    }
    return MeasurementValidation.Valid(
        NewMeasurement(weight, waist, bodyFat, protocolChecks, activityLevel, habits, biochemistry),
    )
}

/** Step 2 · `PUT /consultations/{id}/diagnosis`. */
sealed interface DiagnosisChoice {
    val code: DiagnosisCode

    /** The source the backend stores it with. */
    val source: DiagnosisSource

    /** "Use suggestion" from AI: the generation and its rationale are stored (AI traceability). */
    data class AcceptAiSuggestion(override val code: DiagnosisCode, val aiGenerationId: Long, val rationale: String) :
        DiagnosisChoice {
        override val source: DiagnosisSource get() = DiagnosisSource.AI_SUGGESTION_ACCEPTED
    }

    /** "Choose another", or the rule suggestion: the backend builds the rationale from the measurement if missing. */
    data class Selected(override val code: DiagnosisCode, val rationale: String? = null) : DiagnosisChoice {
        override val source: DiagnosisSource get() = DiagnosisSource.PRACTITIONER_SELECTED
    }

    companion object {
        /** Accepting the suggestion: only a real AI generation counts as "accepted"; a rule one is a choice. */
        fun accepting(suggestion: DiagnosisSuggestion): DiagnosisChoice {
            val generation = suggestion.aiGenerationId
            return if (suggestion.isFromAi && generation != null) {
                AcceptAiSuggestion(suggestion.code, generation, suggestion.rationale)
            } else {
                Selected(suggestion.code, suggestion.rationale)
            }
        }
    }
}

/** "Change parameters" on the targets step. The backend sets the reference weight and activity factor. */
data class TargetParameters(
    val equation: EnergyEquation,
    val deficitKind: DeficitKind,
    val deficitValue: Double,
    val proteinGramsPerKg: Double,
    val fatPercentOfEnergy: Double,
) {
    init {
        require(deficitValue in 0.0..deficitKind.maximum) { "Deficit out of range" }
        require(proteinGramsPerKg in PROTEIN_RANGE) { "Protein per kg out of range" }
        require(fatPercentOfEnergy in FAT_RANGE) { "Fat percent out of range" }
    }

    companion object {
        /** Ranges the phone accepts before requesting the calculation; the backend revalidates everything together. */
        val PROTEIN_RANGE = 0.5..3.0
        val FAT_RANGE = 15.0..45.0
    }
}

/** Step 3 · `PUT /consultations/{id}/targets`. */
sealed interface TargetPrescription {
    /** "Accept targets". */
    data object AcceptedAsProposed : TargetPrescription

    /** "Enter my own values": the reason is required (`OverrideReasonRequired`). */
    data class Overridden(val targets: Targets, val reason: OverrideReason) : TargetPrescription
}

/** Own values: what is missing before they can be saved. */
enum class OverrideField { ENERGY, PROTEIN, CARB, FAT, REASON }

sealed interface OverrideValidation {
    data class Valid(val prescription: TargetPrescription.Overridden) : OverrideValidation
    data class Invalid(val problems: Set<OverrideField>) : OverrideValidation
}

/** Validates "Enter my own values": energy > 0, macros ≥ 0 and a required reason (1–500 characters). */
fun validateOverride(energy: String, protein: String, carb: String, fat: String, reason: String): OverrideValidation {
    val problems = mutableSetOf<OverrideField>()
    fun read(text: String, field: OverrideField, positive: Boolean): Double? {
        val value = DecimalText.parse(text)
        val ok = value != null && (if (positive) value > 0 else value >= 0) && value <= MAX_OVERRIDE_VALUE
        if (!ok) problems += field
        return value.takeIf { ok }
    }
    val e = read(energy, OverrideField.ENERGY, positive = true)
    val p = read(protein, OverrideField.PROTEIN, positive = false)
    val c = read(carb, OverrideField.CARB, positive = false)
    val f = read(fat, OverrideField.FAT, positive = false)
    if (reason.isBlank() || reason.trim().length > OverrideReason.MAX_LENGTH) problems += OverrideField.REASON
    if (problems.isNotEmpty() || e == null || p == null || c == null || f == null) {
        return OverrideValidation.Invalid(problems)
    }
    return OverrideValidation.Valid(TargetPrescription.Overridden(Targets(e, p, c, f), OverrideReason(reason.trim())))
}

private const val MAX_OVERRIDE_VALUE = 10_000.0

/**
 * Step 4 · what gets published and the draft (`publication-draft`). Up to 5 custom guidelines of
 * 3–140 characters each; the message is optional (≤ 500).
 */
data class Publication(
    val restrictions: Set<PlanRestrictionCode>,
    val guidelines: Set<PlanGuidelineCode>,
    val customGuidelines: List<CustomGuideline>,
    val patientMessage: PatientMessage?,
) {
    init {
        require(customGuidelines.size <= CustomGuideline.MAX_PER_VERSION) { "At most 5 custom guidelines" }
    }
}
