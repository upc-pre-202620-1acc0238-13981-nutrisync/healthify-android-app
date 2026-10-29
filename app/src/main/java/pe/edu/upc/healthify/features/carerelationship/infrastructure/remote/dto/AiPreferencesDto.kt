package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `AiPreferencesResource` (older responses lack `mealPhotoRecognitionEnabled`, which means off). It is also the
 * format of the last read saved on the phone.
 */
@Serializable
data class AiPreferencesDto(
    val consentGranted: Boolean = false,
    val weeklySummaryEnabled: Boolean = false,
    val mealIdeasEnabled: Boolean = false,
    val suggestedQuestionsEnabled: Boolean = false,
    val mealPhotoRecognitionEnabled: Boolean = false,
)

/** `UpdateAiPreferencesResource` (`mealPhotoRecognitionEnabled` is always sent). */
@Serializable
data class UpdateAiPreferencesRequestDto(
    val weeklySummaryEnabled: Boolean,
    val mealIdeasEnabled: Boolean,
    val suggestedQuestionsEnabled: Boolean,
    val mealPhotoRecognitionEnabled: Boolean,
)
