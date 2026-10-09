package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Fechas y horas en el idioma de la app y la zona del teléfono (java.time + Locale actual, sin
 * concatenar). Solo formato: ninguna regla de negocio.
 */

/** «4 sept 2026» / «Sep 4, 2026». */
@Composable
@ReadOnlyComposable
fun Instant.mediumDateText(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()).format(atZone(zone))

/** «8:10 a. m.» / «8:10 AM». */
@Composable
@ReadOnlyComposable
fun Instant.shortTimeText(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()).format(atZone(zone))

/** «Jue 18 sept.» / «Thu, Sep 18»: día de la semana abreviado, con mayúscula inicial. */
@Composable
@ReadOnlyComposable
fun Instant.weekdayDayMonthText(zone: ZoneId = ZoneId.systemDefault()): String {
    val locale = currentLocale()
    val pattern = if (locale.language == Locale.ENGLISH.language) "EEE, MMM d" else "EEE d MMM"
    val text = DateTimeFormatter.ofPattern(pattern, locale).format(atZone(zone))
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** Si [this] cae hoy en la zona del teléfono. */
fun Instant.isToday(zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): Boolean =
    atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate()
