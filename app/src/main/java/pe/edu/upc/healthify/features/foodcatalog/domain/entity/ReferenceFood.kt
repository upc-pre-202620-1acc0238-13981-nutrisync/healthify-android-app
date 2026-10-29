package pe.edu.upc.healthify.features.foodcatalog.domain.entity

import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.ReferenceFoodId

/**
 * Reference catalog food (`ReferenceFoodResource`): local name and nutrients per 100 g. It is public
 * reference data; it says nothing about any patient.
 *
 * @param isLocalOverride local food loaded by a nutritionist (comes first in searches).
 */
data class ReferenceFood(
    val id: ReferenceFoodId,
    val name: String,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbGPer100g: Double,
    val fatGPer100g: Double,
    val isLocalOverride: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "A reference food needs a name" }
        require(energyKcalPer100g >= 0) { "Energy cannot be negative" }
        require(proteinGPer100g >= 0 && carbGPer100g >= 0 && fatGPer100g >= 0) { "Nutrients cannot be negative" }
    }
}
