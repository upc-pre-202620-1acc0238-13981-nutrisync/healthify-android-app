package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `MealPhotoAnalysisResource` (IN-7). */
@Serializable
data class MealPhotoAnalysisDto(
    val analysisId: String,
    val referenceFoodId: Long,
    val foodName: String,
    val estimatedGrams: Double,
    val confidence: Double,
    val alternatives: List<MealPhotoAlternativeDto> = emptyList(),
    val expiresAt: String,
)

/** `MealPhotoAlternativeResource` (`referenceFoodId` solo si el catálogo local la resolvió). */
@Serializable
data class MealPhotoAlternativeDto(
    val name: String,
    val grams: Double,
    val referenceFoodId: Long? = null,
)

/** `GenerateMealIdeasResource`. */
@Serializable
data class GenerateMealIdeasRequestDto(
    val localDate: String,
    val excludeIdeaIds: List<String>? = null,
)

/** `MealIdeasResource` (IA-3). */
@Serializable
data class MealIdeasDto(
    val localDate: String,
    val remainingKcal: Double,
    val remainingProteinG: Double = 0.0,
    val remainingCarbG: Double = 0.0,
    val remainingFatG: Double = 0.0,
    val restrictions: List<String> = emptyList(),
    val ideas: List<MealIdeaDto> = emptyList(),
    val disclaimer: String? = null,
)

/** `MealIdeaResource`. */
@Serializable
data class MealIdeaDto(
    val mealIdeaId: String,
    val name: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val ingredients: List<MealIdeaIngredientDto> = emptyList(),
    val why: String = "",
)

/** `MealIdeaIngredientResource` (`resolved = false` → no está en el catálogo, FC-2). */
@Serializable
data class MealIdeaIngredientDto(
    val name: String,
    val grams: Double,
    val referenceFoodId: Long? = null,
    val catalogName: String? = null,
    val resolved: Boolean = true,
)
