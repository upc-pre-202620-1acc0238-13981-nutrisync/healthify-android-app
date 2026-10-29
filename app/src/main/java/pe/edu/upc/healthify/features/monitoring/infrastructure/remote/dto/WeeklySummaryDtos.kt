package pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `WeeklySummaryFactsResource`. */
@Serializable
data class WeeklySummaryFactsDto(
    val metDays: Int = 0,
    val totalDays: Int = 0,
    val loggedDays: Int = 0,
    val unloggedDays: Int = 0,
    val weightChangeKg: Double? = null,
)

/** `WeeklySummaryResource`. */
@Serializable
data class WeeklySummaryDto(
    val weekStart: String,
    val weekEnd: String,
    val headline: String,
    val wentWell: List<String> = emptyList(),
    val watchOut: List<String> = emptyList(),
    val facts: WeeklySummaryFactsDto,
    val language: String? = null,
    val generatedAt: String,
)
