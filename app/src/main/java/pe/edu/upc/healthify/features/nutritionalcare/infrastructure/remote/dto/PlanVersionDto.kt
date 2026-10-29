package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GuidelineItemDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PlanChangeDto

/**
 * `PatientPlanVersionResource`: what the previous versions screen lists and each version's detail. The current one
 * is also read from `active-targets` (which is cached to view offline). Guidelines and changes share their format
 * with `ActiveTargetsResource`.
 */
@Serializable
data class PlanVersionDto(
    val version: Int,
    val publishedAt: String,
    val isActive: Boolean = false,
    val energyKcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val guidelines: List<GuidelineItemDto> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val changesFromPrevious: List<PlanChangeDto> = emptyList(),
    val patientMessage: String? = null,
    val legacyRestrictions: List<String>? = null,
)
