package pe.edu.upc.healthify.core.network

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Fechas del backend en los mappers de `infrastructure`: `DateTimeOffset` llega como ISO-8601 con offset
 * (`2026-09-04T10:00:00-05:00`, hasta 7 decimales) y `DateOnly` como `2026-09-18`. `null` si no se puede leer.
 */
fun String.toInstantOrNull(): Instant? =
    try {
        OffsetDateTime.parse(this).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }

fun String.toOffsetDateTimeOrNull(): OffsetDateTime? =
    try {
        OffsetDateTime.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }

fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
