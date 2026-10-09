package pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.CreateLocalOverrideRequestDto
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.ReferenceFoodId
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodEntity
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.ReferenceFoodDto
import java.text.Normalizer
import java.util.Locale

/** @throws IllegalArgumentException si el recurso rompe una invariante (id ≤ 0, nombre vacío, nutriente < 0). */
fun ReferenceFoodDto.toDomain(): ReferenceFood = ReferenceFood(
    id = ReferenceFoodId(referenceFoodId),
    name = localName.trim(),
    energyKcalPer100g = energyKcalPer100g,
    proteinGPer100g = proteinGPer100g,
    carbGPer100g = carbGPer100g,
    fatGPer100g = fatGPer100g,
    isLocalOverride = isLocalOverride,
)

/** Los recursos inválidos se descartan: un alimento roto no impide mostrar el resto. */
fun List<ReferenceFoodDto>.toDomainSkippingInvalid(): List<ReferenceFood> = mapNotNull {
    try {
        it.toDomain()
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun ReferenceFood.toEntity(position: Int, savedAtEpochMillis: Long): LocalFoodEntity = LocalFoodEntity(
    referenceFoodId = id.value,
    localName = name,
    normalizedName = normalizeFoodName(name),
    energyKcalPer100g = energyKcalPer100g,
    proteinGPer100g = proteinGPer100g,
    carbGPer100g = carbGPer100g,
    fatGPer100g = fatGPer100g,
    isLocalOverride = isLocalOverride,
    position = position,
    savedAtEpochMillis = savedAtEpochMillis,
)

fun LocalFoodEntity.toDomain(): ReferenceFood = ReferenceFood(
    id = ReferenceFoodId(referenceFoodId),
    name = localName,
    energyKcalPer100g = energyKcalPer100g,
    proteinGPer100g = proteinGPer100g,
    carbGPer100g = carbGPer100g,
    fatGPer100g = fatGPer100g,
    isLocalOverride = isLocalOverride,
)

/** Minúsculas, sin tildes y con espacios simples: «Quínua  Cocida» → «quinua cocida». */
fun normalizeFoodName(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(WHITESPACE, " ")

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

fun NewLocalFood.toDto(): CreateLocalOverrideRequestDto =
    CreateLocalOverrideRequestDto(name.trim(), energyKcalPer100g, proteinGPer100g, carbGPer100g, fatGPer100g)
