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
 * Texto que la UI todavía no resolvió: una plantilla de `strings.xml` con sus argumentos (códigos del backend
 * traducidos en la app) o un texto escrito por una persona, que se muestra **tal cual**.
 *
 * Los argumentos pueden ser otros [UiText], `Int`, `String`, `Double`, [DecimalNumber] o [DayMonth]; los `Double` se formatean con el
 * `Locale` actual (hasta 1 decimal) y las fechas como día y mes, nunca concatenando.
 */
sealed interface UiText {

    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Texto libre de una persona (indicación propia, restricción legada, mensaje): nunca se traduce. */
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

        /** Número con el `Locale` dado: separador de miles y hasta 1 decimal («1 850», «62,5»). */
        fun formatNumber(value: Double, locale: Locale): String =
            NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
    }
}

/** Argumento de fecha de un [UiText]: día y mes abreviado en el idioma del lector («8 sept.» / «Sep 8»). */
data class DayMonth(val date: LocalDate) {
    fun format(locale: Locale): String {
        val pattern = if (locale.language == Locale.ENGLISH.language) "MMM d" else "d MMM"
        return DateTimeFormatter.ofPattern(pattern, locale).format(date)
    }
}

/** Argumento numérico con más decimales que el formato por defecto («0,46 kg/semana»). */
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

/** Resuelve el texto con los recursos y el idioma actuales de la composición. */
@Composable
@ReadOnlyComposable
fun UiText.asString(): String = resolve(LocalResources.current, currentLocale())

/** El `Locale` de la configuración actual (el idioma que eligió el paciente en la app). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
