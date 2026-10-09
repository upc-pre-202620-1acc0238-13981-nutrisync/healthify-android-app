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

/** Paso 1 · `PUT /consultations/{id}/measurement` (NC-3). Al menos una casilla del protocolo y la actividad. */
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

    /** `true` si es lo mismo que ya guardó el paso 1: no se vuelve a enviar (repetirlo obliga a rehacer el paso 2). */
    fun sameAs(saved: ConsultationMeasurement): Boolean =
        weightKg == saved.weightKg && waistCm == saved.waistCm && bodyFatPercentage == saved.bodyFatPercentage &&
            protocolChecks == saved.protocolChecks && activityLevel == saved.activityLevel && habits == saved.habits &&
            biochemistry == saved.biochemistry
}

/** Campos de EV-2 que el teléfono valida. */
enum class MeasurementField {
    WEIGHT, WAIST, BODY_FAT, PROTOCOL, ACTIVITY, MEALS_PER_DAY, WATER, MEALS_OUT, GLUCOSE, CHOLESTEROL, TRIGLYCERIDES
}

/** Por qué un campo de EV-2 no sirve: vacío siendo obligatorio o fuera del rango del backend. */
enum class FieldProblem { REQUIRED, OUT_OF_RANGE }

/** El texto de cada campo de EV-2 tal como lo escribió el profesional. */
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

/** Valida EV-2 con los rangos del backend (`ImplausibleMeasurement`, `ProtocolChecklistEmpty`, `InvalidEatingHabits`…). */
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

/** Paso 2 · `PUT /consultations/{id}/diagnosis` (NC-4). */
sealed interface DiagnosisChoice {
    val code: DiagnosisCode

    /** Con qué origen lo guarda el backend. */
    val source: DiagnosisSource

    /** «Usar sugerencia» de la IA: se guarda la generación y su fundamento (trazabilidad IA-0). */
    data class AcceptAiSuggestion(override val code: DiagnosisCode, val aiGenerationId: Long, val rationale: String) :
        DiagnosisChoice {
        override val source: DiagnosisSource get() = DiagnosisSource.AI_SUGGESTION_ACCEPTED
    }

    /** «Elegir otro», o la sugerencia por regla: el backend arma el fundamento con la medición si no hay uno. */
    data class Selected(override val code: DiagnosisCode, val rationale: String? = null) : DiagnosisChoice {
        override val source: DiagnosisSource get() = DiagnosisSource.PRACTITIONER_SELECTED
    }

    companion object {
        /** Aceptar la sugerencia: solo una generación real de IA cuenta como «aceptada»; la de regla es una elección. */
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

/** «Cambiar parámetros» de EV-4 (NC-5). El peso de referencia y el factor de actividad los pone el backend. */
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
        /** Rangos que el teléfono acepta antes de pedir el cálculo; el backend vuelve a validar todo junto. */
        val PROTEIN_RANGE = 0.5..3.0
        val FAT_RANGE = 15.0..45.0
    }
}

/** Paso 3 · `PUT /consultations/{id}/targets` (NC-5). */
sealed interface TargetPrescription {
    /** «Aceptar metas». */
    data object AcceptedAsProposed : TargetPrescription

    /** «Escribir mis propios valores»: la razón es obligatoria (`OverrideReasonRequired`). */
    data class Overridden(val targets: Targets, val reason: OverrideReason) : TargetPrescription
}

/** EV-4 · valores propios: qué falta para poder guardarlos. */
enum class OverrideField { ENERGY, PROTEIN, CARB, FAT, REASON }

sealed interface OverrideValidation {
    data class Valid(val prescription: TargetPrescription.Overridden) : OverrideValidation
    data class Invalid(val problems: Set<OverrideField>) : OverrideValidation
}

/** Valida «Escribir mis propios valores»: energía > 0, macros ≥ 0 y la razón (1–500 caracteres) obligatoria. */
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
 * Paso 4 · lo que se publica (NC-6, NC-7, NC-9) y el borrador (`publication-draft`). Hasta 5 indicaciones propias de
 * 3–140 caracteres cada una; el mensaje es opcional (≤ 500).
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
