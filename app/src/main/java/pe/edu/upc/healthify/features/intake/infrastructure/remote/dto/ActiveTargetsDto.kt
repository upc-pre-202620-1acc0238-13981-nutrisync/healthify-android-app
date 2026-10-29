package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `ActiveTargetsResource`. `guidelines` repeats the guidelines as plain text for older clients; the app uses
 * `guidelineItems` (a code **or** custom text) when present. `legacyRestrictions` are restrictions written
 * before the closed list existed: they are shown as is.
 *
 * It is also the format of the offline copy (Room stores this JSON).
 */
@Serializable
data class ActiveTargetsDto(
    val patientId: Long,
    val planVersion: Int,
    val validFrom: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val guidelines: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val refreshedAt: String? = null,
    val guidelineItems: List<GuidelineItemDto>? = null,
    val legacyRestrictions: List<String>? = null,
    val changesFromPrevious: List<PlanChangeDto>? = null,
    val patientMessage: String? = null,
)

/** `ActiveGuidelineResource(Code, Custom)`: one of the two. */
@Serializable
data class GuidelineItemDto(
    val code: String? = null,
    val custom: String? = null,
)

/** `ActivePlanChangeResource(Type, Code, Custom, Macro, From, To)`. */
@Serializable
data class PlanChangeDto(
    val type: String,
    val code: String? = null,
    val custom: String? = null,
    val macro: String? = null,
    val from: Double? = null,
    val to: Double? = null,
)

/** `DailyIntakeResource` (`GET /patients/{pid}/daily-intake?date=`): what was logged today and what is left. */
@Serializable
data class DailyIntakeDto(
    val date: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val entryCount: Int = 0,
    val remainingEnergyKcal: Double,
    val remainingProteinG: Double,
    val remainingCarbG: Double,
    val remainingFatG: Double,
)
