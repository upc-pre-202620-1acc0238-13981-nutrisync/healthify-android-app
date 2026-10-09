package pe.edu.upc.healthify.features.monitoring.presentation.components

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

/*
 * Códigos de las consultas (MA-2, MA-4, RM-5) → textos de strings.xml en el idioma del lector. Un
 * código desconocido se muestra tal cual (UiText.Raw).
 */

fun PreparationInstruction.toUiText(): UiText = when (this) {
    is PreparationInstruction.Catalog -> UiText.of(code.labelRes)
    is PreparationInstruction.Custom -> UiText.Raw(text)
}

val PreparationCode.labelRes: Int
    @StringRes get() = when (this) {
        PreparationCode.FASTING -> R.string.preparation_fasting
        PreparationCode.LIGHT_CLOTHING -> R.string.preparation_light_clothing
        PreparationCode.BRING_BLOOD_TESTS -> R.string.preparation_bring_blood_tests
        PreparationCode.EMPTY_BLADDER -> R.string.preparation_empty_bladder
    }

val ConsultationModality.labelRes: Int
    @StringRes get() = when (this) {
        ConsultationModality.IN_PERSON -> R.string.modality_in_person
        ConsultationModality.REMOTE -> R.string.modality_remote
    }

/** «Primera consulta» · «Evaluación y nuevo plan (versión 3)». */
fun ConsultationLabel.toUiText(planVersion: Int?): UiText = when (this) {
    ConsultationLabel.FirstConsultation -> UiText.of(R.string.consultation_label_first)
    ConsultationLabel.AssessmentAndNewPlan ->
        if (planVersion != null) {
            UiText.of(R.string.consultation_label_new_plan_version, planVersion)
        } else {
            UiText.of(R.string.consultation_label_new_plan)
        }
    is ConsultationLabel.Custom -> UiText.Raw(text)
}

val PlanFeeling.labelRes: Int
    @StringRes get() = when (this) {
        PlanFeeling.GOOD -> R.string.check_in_feeling_good
        PlanFeeling.FAIR -> R.string.check_in_feeling_fair
        PlanFeeling.HARD -> R.string.check_in_feeling_hard
    }

val CheckInDifficulty.labelRes: Int
    @StringRes get() = when (this) {
        CheckInDifficulty.DINNERS -> R.string.check_in_difficulty_dinners
        CheckInDifficulty.WEEKENDS -> R.string.check_in_difficulty_weekends
        CheckInDifficulty.EATING_OUT -> R.string.check_in_difficulty_eating_out
        CheckInDifficulty.SCHEDULES -> R.string.check_in_difficulty_schedules
        CheckInDifficulty.CRAVINGS -> R.string.check_in_difficulty_cravings
    }

/** «SEPT» / «SEP»: mes abreviado en mayúsculas y sin punto (cuadro de la fecha). */
internal fun shortMonthText(date: TemporalAccessor, locale: Locale): String =
    DateTimeFormatter.ofPattern("MMM", locale).format(date).replace(".", "").uppercase(locale)

/** «Jueves» / «Thursday». */
internal fun weekdayText(date: TemporalAccessor, locale: Locale): String =
    DateTimeFormatter.ofPattern("EEEE", locale).format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

/** «4 de septiembre» / «September 4». */
internal fun dayMonthText(date: TemporalAccessor, locale: Locale): String =
    DateTimeFormatter.ofPattern(if (locale.language == Locale.ENGLISH.language) "MMMM d" else "d 'de' MMMM", locale)
        .format(date)

/** «3 de septiembre de 2026» / «September 3, 2026». */
internal fun longDateText(date: TemporalAccessor, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
