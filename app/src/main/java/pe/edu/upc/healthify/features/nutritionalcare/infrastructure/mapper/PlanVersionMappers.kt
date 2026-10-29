package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanVersionDto

/** `null` if the version breaks an invariant (unreadable date, non-positive version or energy): it is not shown. */
fun PlanVersionDto.toDomainOrNull(): PlanVersion? {
    val published = publishedAt.toInstantOrNull() ?: return null
    if (version <= 0 || energyKcal <= 0) return null
    return PlanVersion(
        version = version,
        publishedAt = published,
        isActive = isActive,
        energyKcal = energyKcal,
        proteinG = proteinG,
        carbG = carbG,
        fatG = fatG,
        guidelines = guidelines.mapNotNull { PlanGuideline.of(it.code, it.custom) }.distinct(),
        restrictions = (
            restrictions.mapNotNull(DietaryRestriction::ofCode) +
                legacyRestrictions.orEmpty().mapNotNull(DietaryRestriction::legacy)
            ).distinct(),
        changesFromPrevious = changesFromPrevious.mapNotNull {
            PlanChange.of(it.type, it.code, it.custom, it.macro, it.from, it.to)
        },
        patientMessage = patientMessage?.trim()?.takeIf { it.isNotEmpty() },
    )
}
