package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Diary formats: numbers and times with the current Locale, never by concatenation.
 */

/** "280", "62,5". */
@Composable
@ReadOnlyComposable
fun numberText(value: Double): String = UiText.formatNumber(value, currentLocale())

/** "280 g approx." (proposal) or "280 g" (confirmed). */
@Composable
@ReadOnlyComposable
fun gramsText(grams: Double, approximate: Boolean): String =
    stringResource(if (approximate) R.string.diary_grams_approx else R.string.format_grams, numberText(grams))

/** "2:10 p.m.": the time the patient declared, in their own offset (no time zone conversion). */
@Composable
@ReadOnlyComposable
fun OffsetDateTime.clockTimeText(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()).format(this)

/** Confidence 0–1 → whole percentage (0.64 → 64). */
fun confidencePercent(confidence: Double): Int = (confidence * PERCENT).roundToInt().coerceIn(0, PERCENT.toInt())

/** "9 de septiembre" / "September 9". */
fun dayMonthText(date: LocalDate, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "MMMM d" else "d 'de' MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

/** "Miércoles, 7 de septiembre" / "Wednesday, September 7". */
fun weekdayDayMonthText(date: LocalDate, locale: Locale): String {
    val pattern = if (locale.language == Locale.ENGLISH.language) "EEEE, MMMM d" else "EEEE, d 'de' MMMM"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** Diary date picker: "Today, September 9", "Yesterday, September 8" or "Monday, September 7". */
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

/** "When did you eat?" field: "Today · 1:15 p.m.", "Yesterday · 9:30 p.m." or "Monday, September 7 · 8:00 a.m.". */
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

/** Sentence for each restriction on the meal ideas screen ("…and contain no seafood"). */
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

/** Restriction name ("No seafood"); a legacy or unknown one is shown as is. */
@Composable
@ReadOnlyComposable
fun restrictionName(restriction: DietaryRestriction): String = when (restriction) {
    is DietaryRestriction.Legacy -> restriction.text
    is DietaryRestriction.Catalog -> stringResource(
        when (restriction.code) {
            RestrictionCode.LACTOSE_FREE -> R.string.restriction_lactose_free
            RestrictionCode.GLUTEN_FREE -> R.string.restriction_gluten_free
            RestrictionCode.VEGAN -> R.string.restriction_vegan
            RestrictionCode.VEGETARIAN -> R.string.restriction_vegetarian
            RestrictionCode.TREE_NUT_FREE -> R.string.restriction_tree_nut_free
            RestrictionCode.SHELLFISH_FREE -> R.string.restriction_shellfish_free
            RestrictionCode.KOSHER -> R.string.restriction_kosher
            RestrictionCode.HALAL -> R.string.restriction_halal
        },
    )
}

/** "contain no seafood", "contain no gluten and contain no dairy", "a, b and c" (`null` if there are none). */
fun restrictionsPhrase(restrictions: List<RestrictionCode>): UiText? {
    val phrases = restrictions.map { UiText.of(it.ideasPhraseRes) }
    if (phrases.isEmpty()) return null
    if (phrases.size == 1) return phrases.single()
    val head = phrases.dropLast(1).reduce { acc, next -> UiText.of(R.string.ideas_join, acc, next) }
    return UiText.of(R.string.ideas_join_last, head, phrases.last())
}

private const val PERCENT = 100.0
