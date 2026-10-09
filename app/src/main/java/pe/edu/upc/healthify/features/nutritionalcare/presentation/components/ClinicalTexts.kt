package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanRestrictionItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import java.text.NumberFormat
import java.util.Locale

/*
 * Códigos clínicos del lado del nutricionista → textos de strings.xml (X-2). Los textos escritos por una persona
 * (indicación propia, restricción legada, mensaje, preguntas del paciente) se muestran tal cual.
 */

/** «Femenino» / «Masculino» del selector de EV-1. */
@StringRes
fun BiologicalSex.optionRes(): Int = when (this) {
    BiologicalSex.FEMALE -> R.string.sex_female
    BiologicalSex.MALE -> R.string.sex_male
}

/** «Mujer» / «Hombre» de los resúmenes («Mujer · 31 años · 168 cm»). */
@StringRes
fun BiologicalSex.summaryRes(): Int = when (this) {
    BiologicalSex.FEMALE -> R.string.sex_summary_female
    BiologicalSex.MALE -> R.string.sex_summary_male
}

@StringRes
fun MedicalCondition.labelRes(): Int = when (this) {
    MedicalCondition.TYPE_2_DIABETES -> R.string.condition_type_2_diabetes
    MedicalCondition.HYPERTENSION -> R.string.condition_hypertension
    MedicalCondition.CELIAC_DISEASE -> R.string.condition_celiac_disease
    MedicalCondition.HYPOTHYROIDISM -> R.string.condition_hypothyroidism
    MedicalCondition.CHRONIC_KIDNEY_DISEASE -> R.string.condition_chronic_kidney_disease
    MedicalCondition.GOUT -> R.string.condition_gout
}

@StringRes
fun ActivityLevel.labelRes(): Int = when (this) {
    ActivityLevel.SEDENTARY -> R.string.activity_sedentary
    ActivityLevel.LIGHT -> R.string.activity_light
    ActivityLevel.MODERATE -> R.string.activity_moderate
    ActivityLevel.INTENSE -> R.string.activity_intense
}

/** «actividad moderada» dentro de una frase. */
@StringRes
fun ActivityLevel.inlineRes(): Int = when (this) {
    ActivityLevel.SEDENTARY -> R.string.activity_inline_sedentary
    ActivityLevel.LIGHT -> R.string.activity_inline_light
    ActivityLevel.MODERATE -> R.string.activity_inline_moderate
    ActivityLevel.INTENSE -> R.string.activity_inline_intense
}

@StringRes
fun ProtocolCheck.labelRes(): Int = when (this) {
    ProtocolCheck.FASTING -> R.string.protocol_fasting
    ProtocolCheck.NO_SHOES -> R.string.protocol_no_shoes
    ProtocolCheck.LIGHT_CLOTHING -> R.string.protocol_light_clothing
    ProtocolCheck.EMPTY_BLADDER -> R.string.protocol_empty_bladder
    ProtocolCheck.SAME_SCALE -> R.string.protocol_same_scale
}

@StringRes
fun DiagnosisCode.labelRes(): Int = when (this) {
    DiagnosisCode.UNDERWEIGHT -> R.string.diagnosis_underweight
    DiagnosisCode.NORMAL_WEIGHT -> R.string.diagnosis_normal_weight
    DiagnosisCode.OVERWEIGHT_GRADE_I -> R.string.diagnosis_overweight_grade_i
    DiagnosisCode.OBESITY_GRADE_I -> R.string.diagnosis_obesity_grade_i
    DiagnosisCode.OBESITY_GRADE_II -> R.string.diagnosis_obesity_grade_ii
    DiagnosisCode.OBESITY_GRADE_III -> R.string.diagnosis_obesity_grade_iii
}

/** «Medición de hoy», «Diagnóstico», «Metas», «Indicaciones y publicación». */
@StringRes
fun ConsultationStep.titleRes(): Int = when (this) {
    ConsultationStep.MEASUREMENT -> R.string.consultation_step_measurement
    ConsultationStep.DIAGNOSIS -> R.string.consultation_step_diagnosis
    ConsultationStep.TARGETS -> R.string.consultation_step_targets
    ConsultationStep.PUBLICATION -> R.string.consultation_step_publication
}

@StringRes
fun PlanRestrictionCode.labelRes(): Int = when (this) {
    PlanRestrictionCode.LACTOSE_FREE -> R.string.restriction_lactose_free
    PlanRestrictionCode.GLUTEN_FREE -> R.string.restriction_gluten_free
    PlanRestrictionCode.VEGAN -> R.string.restriction_vegan
    PlanRestrictionCode.VEGETARIAN -> R.string.restriction_vegetarian
    PlanRestrictionCode.TREE_NUT_FREE -> R.string.restriction_tree_nut_free
    PlanRestrictionCode.SHELLFISH_FREE -> R.string.restriction_shellfish_free
    PlanRestrictionCode.KOSHER -> R.string.restriction_kosher
    PlanRestrictionCode.HALAL -> R.string.restriction_halal
}

@StringRes
fun PlanGuidelineCode.labelRes(): Int = when (this) {
    PlanGuidelineCode.PRIORITIZE_VEGETABLES -> R.string.guideline_prioritize_vegetables
    PlanGuidelineCode.DRINK_2L_WATER -> R.string.guideline_drink_2l_water
    PlanGuidelineCode.AVOID_SUGARY_DRINKS -> R.string.guideline_avoid_sugary_drinks
    PlanGuidelineCode.PROTEIN_AT_BREAKFAST -> R.string.guideline_protein_at_breakfast
    PlanGuidelineCode.REDUCE_SALT -> R.string.guideline_reduce_salt
    PlanGuidelineCode.EAT_EVERY_3_TO_4_HOURS -> R.string.guideline_eat_every_3_to_4_hours
    PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER -> R.string.guideline_protein_and_vegetables_at_dinner
}

@StringRes
fun EnergyEquation.labelRes(): Int = when (this) {
    EnergyEquation.MIFFLIN_ST_JEOR -> R.string.equation_mifflin_st_jeor
    EnergyEquation.HARRIS_BENEDICT -> R.string.equation_harris_benedict
    EnergyEquation.FAO_WHO_UNU -> R.string.equation_fao_who_unu
    EnergyEquation.KATCH_MCARDLE -> R.string.equation_katch_mcardle
}

@Composable
fun PlanGuidelineItem.text(): String = when (this) {
    is PlanGuidelineItem.Catalog -> stringResource(code.labelRes())
    is PlanGuidelineItem.Custom -> text
}

@Composable
fun PlanRestrictionItem.text(): String = when (this) {
    is PlanRestrictionItem.Catalog -> stringResource(code.labelRes())
    is PlanRestrictionItem.Legacy -> text
}

/** Un número con [decimals] decimales en el idioma de la app («74,2» en español). */
fun formatDecimal(value: Double, decimals: Int, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)

/** Igual que [formatDecimal], sin decimales de más («168» y no «168,0»; «74,25» se queda). */
fun formatCompact(value: Double, maxDecimals: Int, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = maxDecimals
    }.format(value)

@Composable
fun decimalText(value: Double, decimals: Int = 1): String = formatDecimal(value, decimals, currentLocale())

@Composable
fun compactText(value: Double, maxDecimals: Int = 1): String = formatCompact(value, maxDecimals, currentLocale())

/** «Mujer · 31 años · 168 cm». */
@Composable
fun BaselineSummary.headline(): String {
    val sexText = sex?.let { stringResource(it.summaryRes()) }
    val age = pluralStringResource(R.plurals.baseline_age_years, ageYears, ageYears)
    val height = stringResource(R.string.unit_cm_value, compactText(heightCm))
    return listOfNotNull(sexText, age, height).joinToString(stringResource(R.string.text_separator))
}

/** «hipotiroidismo, gota» (en minúscula, dentro de una frase) o `null` sin antecedentes. */
@Composable
fun BaselineSummary.conditionsInline(): String? {
    if (conditions.isEmpty()) return null
    val locale = currentLocale()
    return MedicalCondition.entries.filter { it in conditions }
        .map { stringResource(it.labelRes()).lowercase(locale) }
        .joinToString(stringResource(R.string.list_separator))
}

/** Déficit «de 500 kcal» o «del 20 %». */
@Composable
fun deficitText(kind: DeficitKind?, value: Double): String = when (kind) {
    DeficitKind.PERCENT_OF_TDEE -> stringResource(R.string.targets_deficit_percent, compactText(value))
    else -> stringResource(R.string.targets_deficit_kcal, compactText(value, 0))
}

/**
 * Las viñetas de «Antes de la consulta, Ana contó» (MA-4): el sentir, lo que le costó y sus preguntas (texto del
 * paciente, tal cual). Un código desconocido se omite.
 */
@Composable
fun checkInLines(checkIn: PatientCheckIn): List<String> {
    val locale = currentLocale()
    val feeling = when (checkIn.feeling.lowercase(Locale.ROOT)) {
        "good" -> stringResource(R.string.check_in_report_feeling_good)
        "fair" -> stringResource(R.string.check_in_report_feeling_fair)
        "hard" -> stringResource(R.string.check_in_report_feeling_hard)
        else -> null
    }
    val difficulties = checkIn.difficulties.mapNotNull { code ->
        when (code.lowercase(Locale.ROOT)) {
            "dinners" -> R.string.check_in_difficulty_dinners
            "weekends" -> R.string.check_in_difficulty_weekends
            "eatingout" -> R.string.check_in_difficulty_eating_out
            "schedules" -> R.string.check_in_difficulty_schedules
            "cravings" -> R.string.check_in_difficulty_cravings
            else -> null
        }?.let { stringResource(it).lowercase(locale) }
    }
    val hard = difficulties.takeIf { it.isNotEmpty() }?.let {
        stringResource(R.string.check_in_report_difficulties, it.joinToString(stringResource(R.string.list_separator)))
    }
    val questions = checkIn.questions.map { stringResource(R.string.check_in_report_question, it) }
    return listOfNotNull(feeling, hard) + questions
}
