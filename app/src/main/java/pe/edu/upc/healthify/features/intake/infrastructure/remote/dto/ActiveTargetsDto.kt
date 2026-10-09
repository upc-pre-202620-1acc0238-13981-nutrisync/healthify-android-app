package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `ActiveTargetsResource` (§5.5, NC-6/8/9). `guidelines` repite las indicaciones como texto plano para clientes
 * viejos; la app usa `guidelineItems` (código **o** texto propio) cuando llega. `legacyRestrictions` son
 * restricciones escritas antes de la lista cerrada: se muestran tal cual.
 *
 * También es el formato de la copia offline (Room guarda este JSON).
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

/** `ActiveGuidelineResource(Code, Custom)`: uno de los dos. */
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
