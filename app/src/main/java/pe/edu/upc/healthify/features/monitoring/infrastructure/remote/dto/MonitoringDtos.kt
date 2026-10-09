package pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `DailyComplianceResource` (forma de lista, `?date=`). */
@Serializable
data class DailyComplianceDto(
    val date: String,
    val outcome: String,
    val observedEnergyKcal: Double = 0.0,
    val targetEnergyKcal: Double = 0.0,
    val planVersion: Int = 0,
    val entryCount: Int = 0,
)

/** `DailyComplianceRangeResource` (MA-6, `?from=&to=`). */
@Serializable
data class DailyComplianceRangeDto(
    val from: String,
    val to: String,
    val days: List<ComplianceDayDto> = emptyList(),
    val summary: ComplianceSummaryDto,
)

@Serializable
data class ComplianceDayDto(
    val date: String,
    val outcome: String,
)

@Serializable
data class ComplianceSummaryDto(
    val metDays: Int = 0,
    val exceededDays: Int = 0,
    val shortDays: Int = 0,
    val unloggedDays: Int = 0,
    val loggedDays: Int = 0,
    val totalDays: Int = 0,
)

/** `ConsistencyIndexResource` (F23, MA-7). Solo los campos que la app usa; el resto se ignora. */
@Serializable
data class ConsistencyIndexDto(
    val patientId: Long,
    val state: String,
    val patientPromptPending: Boolean = false,
)

/** `PatientFollowUpResource` (MA-3). */
@Serializable
data class PatientFollowUpDto(
    val followUpId: Long,
    val scheduledFor: String,
    val modality: String? = null,
    val preparation: List<String> = emptyList(),
    val scheduledAt: String? = null,
    val practitionerFullName: String? = null,
    val state: String? = null,
)
