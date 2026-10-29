package pe.edu.upc.healthify.core.network

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Backend dates in the `infrastructure` mappers: `DateTimeOffset` arrives as ISO-8601 with offset
 * (`2026-09-04T10:00:00-05:00`, up to 7 decimals) and `DateOnly` as `2026-09-18`. `null` if it cannot be parsed.
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
