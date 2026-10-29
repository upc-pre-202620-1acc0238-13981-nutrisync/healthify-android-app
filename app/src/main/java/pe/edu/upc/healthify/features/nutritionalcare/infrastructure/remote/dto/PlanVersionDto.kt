package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GuidelineItemDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PlanChangeDto

/**
 * `PatientPlanVersionResource` (NC-8): lo que PT4.1 lista y el detalle de cada versión. La vigente se lee además de
 * `active-targets` (que queda guardado para verla sin conexión). Las indicaciones y cambios comparten formato con
 * `ActiveTargetsResource`.
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
