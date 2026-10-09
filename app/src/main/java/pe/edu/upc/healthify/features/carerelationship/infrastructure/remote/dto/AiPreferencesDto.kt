package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `AiPreferencesResource` (IA-1; `mealPhotoRecognitionEnabled` desde IN-7, antes faltaba = apagado). También es el
 * formato de la última lectura guardada en el teléfono.
 */
@Serializable
data class AiPreferencesDto(
    val consentGranted: Boolean = false,
    val weeklySummaryEnabled: Boolean = false,
    val mealIdeasEnabled: Boolean = false,
    val suggestedQuestionsEnabled: Boolean = false,
    val mealPhotoRecognitionEnabled: Boolean = false,
)

/** `UpdateAiPreferencesResource` (IA-1; `mealPhotoRecognitionEnabled` siempre se manda, IN-7). */
@Serializable
data class UpdateAiPreferencesRequestDto(
    val weeklySummaryEnabled: Boolean,
    val mealIdeasEnabled: Boolean,
    val suggestedQuestionsEnabled: Boolean,
    val mealPhotoRecognitionEnabled: Boolean,
)
