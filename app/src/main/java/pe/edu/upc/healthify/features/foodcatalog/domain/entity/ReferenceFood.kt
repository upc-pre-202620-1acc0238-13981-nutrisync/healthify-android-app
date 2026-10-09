package pe.edu.upc.healthify.features.foodcatalog.domain.entity

import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.ReferenceFoodId

/**
 * Alimento del catálogo de referencia (`ReferenceFoodResource`): nombre local y nutrientes por 100 g. Es un dato
 * público de referencia; no dice nada de ningún paciente.
 *
 * @param isLocalOverride alimento local que cargó un nutricionista (va primero en las búsquedas).
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
