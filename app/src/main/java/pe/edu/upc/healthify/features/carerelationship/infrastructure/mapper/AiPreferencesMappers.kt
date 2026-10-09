package pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AiPreferencesDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.UpdateAiPreferencesRequestDto

fun AiPreferencesDto.toDomain(): AiPreferences = AiPreferences(
    consentGranted = consentGranted,
    weeklySummaryEnabled = weeklySummaryEnabled,
    mealIdeasEnabled = mealIdeasEnabled,
    suggestedQuestionsEnabled = suggestedQuestionsEnabled,
    mealPhotoRecognitionEnabled = mealPhotoRecognitionEnabled,
)

fun AiPreferences.toUpdateDto(): UpdateAiPreferencesRequestDto = UpdateAiPreferencesRequestDto(
    weeklySummaryEnabled = weeklySummaryEnabled,
    mealIdeasEnabled = mealIdeasEnabled,
    suggestedQuestionsEnabled = suggestedQuestionsEnabled,
    mealPhotoRecognitionEnabled = mealPhotoRecognitionEnabled,
)
