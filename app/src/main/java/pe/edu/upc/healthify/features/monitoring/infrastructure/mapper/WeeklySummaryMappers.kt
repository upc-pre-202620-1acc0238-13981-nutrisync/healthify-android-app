package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryFacts
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.WeeklySummaryDto

/** `null` si el recurso rompe una invariante (fechas ilegibles, titular vacío, cifras imposibles). */
fun WeeklySummaryDto.toDomainOrNull(): WeeklySummary? {
    val start = weekStart.toLocalDateOrNull() ?: return null
    val end = weekEnd.toLocalDateOrNull() ?: return null
    val generated = generatedAt.toInstantOrNull() ?: return null
    return try {
        WeeklySummary(
            weekStart = start,
            weekEnd = end,
            headline = headline.trim(),
            wentWell = wentWell.map { it.trim() }.filter { it.isNotEmpty() },
            watchOut = watchOut.map { it.trim() }.filter { it.isNotEmpty() },
            facts = WeeklySummaryFacts(
                metDays = facts.metDays,
                totalDays = facts.totalDays,
                loggedDays = facts.loggedDays,
                unloggedDays = facts.unloggedDays,
                weightChangeKg = facts.weightChangeKg,
            ),
            generatedAt = generated,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}
