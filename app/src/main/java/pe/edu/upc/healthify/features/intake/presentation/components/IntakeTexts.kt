package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Formatos del diario: números y horas con el Locale actual, nunca concatenando.
 */

/** «280», «62,5». */
@Composable
@ReadOnlyComposable
fun numberText(value: Double): String = UiText.formatNumber(value, currentLocale())

/** «280 g aprox.» (propuesta) o «280 g» (confirmado). */
@Composable
@ReadOnlyComposable
fun gramsText(grams: Double, approximate: Boolean): String =
    stringResource(if (approximate) R.string.diary_grams_approx else R.string.format_grams, numberText(grams))

/** «2:10 p. m.»: la hora que declaró el paciente, en su propio offset (no se convierte de zona). */
@Composable
@ReadOnlyComposable
fun OffsetDateTime.clockTimeText(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()).format(this)

/** Confianza 0–1 → porcentaje entero (0,64 → 64). */
fun confidencePercent(confidence: Double): Int = (confidence * PERCENT).roundToInt().coerceIn(0, PERCENT.toInt())

/** «9 de septiembre» / «September 9». */
fun dayMonthText(date: LocalDate, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "MMMM d" else "d 'de' MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

/** «Miércoles, 7 de septiembre» / «Wednesday, September 7». */
fun weekdayDayMonthText(date: LocalDate, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "EEEE, MMMM d" else "EEEE, d 'de' MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** Selector de fecha de PT14: «Hoy, 9 de septiembre», «Ayer, 8 de septiembre» o «Lunes, 7 de septiembre». */
@Composable
@ReadOnlyComposable
fun diaryDateText(date: LocalDate, today: LocalDate): String {
    val locale = currentLocale()
    return when (date) {
        today -> stringResource(R.string.diary_date_today, dayMonthText(date, locale))
        today.minusDays(1) -> stringResource(R.string.diary_date_yesterday, dayMonthText(date, locale))
        else -> weekdayDayMonthText(date, locale)
    }
}

/** Campo «¿Cuándo comiste?»: «Hoy · 1:15 p. m.», «Ayer · 9:30 p. m.» o «Lunes, 7 de septiembre · 8:00 a. m.». */
@Composable
@ReadOnlyComposable
fun mealTimeText(value: OffsetDateTime, today: LocalDate): String {
    val time = value.clockTimeText()
    val date = value.toLocalDate()
    return when (date) {
        today -> stringResource(R.string.meal_time_today, time)
        today.minusDays(1) -> stringResource(R.string.meal_time_yesterday, time)
        else -> stringResource(R.string.format_separator_dot, weekdayDayMonthText(date, currentLocale()), time)
    }
}

/** Frase de cada restricción para PT14.4 («…y no llevan mariscos»). */
@get:StringRes
val RestrictionCode.ideasPhraseRes: Int
    get() = when (this) {
        RestrictionCode.LACTOSE_FREE -> R.string.ideas_restriction_lactose_free
        RestrictionCode.GLUTEN_FREE -> R.string.ideas_restriction_gluten_free
        RestrictionCode.VEGAN -> R.string.ideas_restriction_vegan
        RestrictionCode.VEGETARIAN -> R.string.ideas_restriction_vegetarian
        RestrictionCode.TREE_NUT_FREE -> R.string.ideas_restriction_tree_nut_free
        RestrictionCode.SHELLFISH_FREE -> R.string.ideas_restriction_shellfish_free
        RestrictionCode.KOSHER -> R.string.ideas_restriction_kosher
        RestrictionCode.HALAL -> R.string.ideas_restriction_halal
    }

/** «no llevan mariscos», «no llevan gluten y no llevan lácteos», «a, b y c» (`null` si no hay ninguna). */
fun restrictionsPhrase(restrictions: List<RestrictionCode>): UiText? {
    val phrases = restrictions.map { UiText.of(it.ideasPhraseRes) }
    if (phrases.isEmpty()) return null
    if (phrases.size == 1) return phrases.single()
    val head = phrases.dropLast(1).reduce { acc, next -> UiText.of(R.string.ideas_join, acc, next) }
    return UiText.of(R.string.ideas_join_last, head, phrases.last())
}

private const val PERCENT = 100.0
