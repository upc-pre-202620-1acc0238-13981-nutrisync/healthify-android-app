package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanVersionDto

/** `null` si la versión rompe una invariante (fecha ilegible, versión o energía no positivas): no se muestra. */
fun PlanVersionDto.toDomainOrNull(): PlanVersion? {
    val published = publishedAt.toInstantOrNull() ?: return null
    if (version <= 0 || energyKcal <= 0) return null
    return PlanVersion(version = version, publishedAt = published, isActive = isActive, energyKcal = energyKcal)
}
