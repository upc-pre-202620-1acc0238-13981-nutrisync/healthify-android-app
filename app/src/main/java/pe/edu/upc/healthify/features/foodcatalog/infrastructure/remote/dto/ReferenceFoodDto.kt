package pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `ReferenceFoodResource` (§5.4). */
@Serializable
data class ReferenceFoodDto(
    val referenceFoodId: Long,
    val localName: String,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbGPer100g: Double,
    val fatGPer100g: Double,
    val isLocalOverride: Boolean = false,
)

/** `CreateLocalOverrideResource` (F14c). */
@Serializable
data class CreateLocalOverrideRequestDto(
    val localName: String,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbGPer100g: Double,
    val fatGPer100g: Double,
)
