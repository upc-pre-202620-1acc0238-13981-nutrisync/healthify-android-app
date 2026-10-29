package pe.edu.upc.healthify.core.designsystem.component

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Text the UI has not resolved yet: a `strings.xml` template with its arguments (backend codes
 * translated in the app) or text written by a person, shown **as is**.
 *
 * Arguments can be other [UiText], `Int`, `String`, `Double`, [DecimalNumber] or [DayMonth]; `Double`s are formatted with
 * the current `Locale` (up to 1 decimal) and dates as day and month, never by concatenation.
 */
sealed interface UiText {

    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Free text from a person (custom guideline, legacy restriction, message): never translated. */
    data class Raw(val text: String) : UiText

    fun resolve(resources: Resources, locale: Locale): String = when (this) {
        is Raw -> text
        is Resource -> if (args.isEmpty()) {
            resources.getString(id)
        } else {
            resources.getString(id, *args.map { it.resolveArg(resources, locale) }.toTypedArray())
        }
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())

        /** Number in the given `Locale`: thousands separator and up to 1 decimal («1 850», «62,5»). */
        fun formatNumber(value: Double, locale: Locale): String =
            NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
    }
}

/** Date argument of a [UiText]: day and abbreviated month in the reader's language («8 sept.» / «Sep 8»). */
data class DayMonth(val date: LocalDate) {
    fun format(locale: Locale): String {
        val pattern = if (locale.language == Locale.ENGLISH.language) "MMM d" else "d MMM"
        return DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}

/** Numeric argument with more decimals than the default format (e.g. «0.46 kg/week»). */
data class DecimalNumber(val value: Double, val maxFractionDigits: Int) {
    fun format(locale: Locale): String =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = maxFractionDigits }.format(value)
}

private fun Any.resolveArg(resources: Resources, locale: Locale): Any = when (this) {
    is UiText -> resolve(resources, locale)
    is DayMonth -> format(locale)
    is DecimalNumber -> format(locale)
    is Double -> UiText.formatNumber(this, locale)
    is Float -> UiText.formatNumber(toDouble(), locale)
    else -> this
}

/** Resolves the text with the composition's current resources and language. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalResources.current, currentLocale())

/** The current configuration's `Locale` (the language the patient chose in the app). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
