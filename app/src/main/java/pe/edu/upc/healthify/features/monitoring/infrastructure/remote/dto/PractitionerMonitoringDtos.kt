package pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PanelWeekDayDto(val date: String, val outcome: String)

@Serializable
data class PanelWeekDto(
    val days: List<PanelWeekDayDto> = emptyList(),
    val metDays: Int = 0,
    val totalDays: Int = 0,
)

@Serializable
data class PanelLoggedDaysDto(val logged: Int, val totalDays: Int)

@Serializable
data class PanelWeightTrendSummaryDto(val weeks: Int = 0, val slopeKgPerWeek: Double? = null)

@Serializable
data class PanelClinicalMeasurementDto(
    val takenAt: String,
    val weightKg: Double,
    val protocolChecks: List<String> = emptyList(),
)

@Serializable
data class PanelDiaryEntryDto(
    val diaryEntryId: Long,
    val localTimestamp: String,
    val provenance: String? = null,
    val proposedFoodName: String? = null,
    val proposedPortionGrams: Double? = null,
    val confirmedFoodName: String? = null,
    val confirmedPortionGrams: Double? = null,
    val confirmedAt: String? = null,
    val foodName: String? = null,
)

/** `PatientMonitoringPanelResource` (RM-3). */
@Serializable
data class PatientMonitoringPanelDto(
    val date: String,
    val diary: List<PanelDiaryEntryDto> = emptyList(),
    val week: PanelWeekDto? = null,
    val loggedDays: PanelLoggedDaysDto? = null,
    val weightTrendSummary: PanelWeightTrendSummaryDto? = null,
    val lastClinicalMeasurement: PanelClinicalMeasurementDto? = null,
)

/** `MonitoringSummaryResource` (IA-5). */
@Serializable
data class MonitoringSummaryDto(val text: String? = null)
