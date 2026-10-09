package pe.edu.upc.healthify.features.intake.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.core.network.toOffsetDateTimeOrNull
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrendPoint
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PendingSelfWeighInDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.RecordSelfWeighInRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.WeightTrendDto
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedSelfWeighInPayload
import java.time.Instant

fun NewSelfWeighIn.toRequestDto(patientId: Long) = RecordSelfWeighInRequestDto(
    patientId = patientId,
    valueKg = weight.value,
    localTimestamp = localTimestamp.toIsoString(),
    fastedState = fasted,
)

fun NewSelfWeighIn.toQueuedPayload() = QueuedSelfWeighInPayload(
    clientEntryId = clientEntryId.value,
    valueKg = weight.value,
    localTimestamp = localTimestamp.toIsoString(),
    fastedState = fasted,
)

/** El momento se reenvía con el texto exacto que se guardó (nunca se reescribe). */
fun QueuedSelfWeighInPayload.toPendingDto() = PendingSelfWeighInDto(
    clientEntryId = clientEntryId,
    valueKg = valueKg,
    localTimestamp = localTimestamp,
    fastedState = fastedState,
)

/** `null` si la copia guardada ya no se puede leer (la fila se ignora en PT19, no rompe la lista). */
fun QueuedSelfWeighInPayload.toDomainOrNull(rejectionCode: String?): PendingSelfWeighIn? {
    val timestamp = localTimestamp.toOffsetDateTimeOrNull() ?: return null
    val weight = WeightKg.ofOrNull(valueKg) ?: return null
    return try {
        PendingSelfWeighIn(
            clientEntryId = ClientEntryId(clientEntryId),
            weight = weight,
            localTimestamp = LocalTimestamp.restore(timestamp),
            fasted = fastedState,
            rejectionCode = rejectionCode,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * `null` si el recurso rompe una invariante (fechas ilegibles, puntos desordenados…). Los puntos con fecha ilegible
 * invalidan toda la serie: dibujar una línea con huecos inventados sería peor que no dibujarla.
 */
fun WeightTrendDto.toDomainOrNull(fromCache: Boolean = false, savedAt: Instant? = null): WeightTrend? {
    val recalculatedAt = lastRecalculatedAt.toInstantOrNull() ?: return null
    val parsedPoints = points.map { point ->
        val date = point.date.toLocalDateOrNull() ?: return null
        date to point.smoothedValueKg
    }
    return try {
        WeightTrend(
            points = parsedPoints.map { (date, kg) -> WeightTrendPoint(date, kg) },
            windowSize = windowSize,
            lastRecalculatedAt = recalculatedAt,
            excludedReadingsCount = excludedReadingsCount,
            changeKgOverRange = changeKgOverRange,
            slopeKgPerWeek = slopeKgPerWeek,
            rangeFrom = rangeFrom?.toLocalDateOrNull(),
            rangeTo = rangeTo?.toLocalDateOrNull(),
            fromCache = fromCache,
            savedAt = savedAt,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}
