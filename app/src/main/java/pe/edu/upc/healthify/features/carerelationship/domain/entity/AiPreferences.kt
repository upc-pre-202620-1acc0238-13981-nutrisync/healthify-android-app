package pe.edu.upc.healthify.features.carerelationship.domain.entity

/**
 * The patient's AI consent and per-feature preferences (`AiPreferencesResource`). An AI feature is available
 * only if AI consent is still current **and** its preference is on.
 * Whether AI is turned off on the server (`503 AiFeatureDisabled`) is not known here: each feature reports it.
 */
/** The AI features the patient turns on or off in the AI features settings. */
enum class AiFeature { WEEKLY_SUMMARY, MEAL_IDEAS, SUGGESTED_QUESTIONS, MEAL_PHOTO_RECOGNITION }

data class AiPreferences(
    val consentGranted: Boolean,
    val weeklySummaryEnabled: Boolean,
    val mealIdeasEnabled: Boolean,
    val suggestedQuestionsEnabled: Boolean,
    val mealPhotoRecognitionEnabled: Boolean,
) {
    /** "Log meal" opens the camera; otherwise it goes straight to manual logging. */
    val canRecognizeMealPhotos: Boolean get() = consentGranted && mealPhotoRecognitionEnabled

    /** The "Not sure what to eat?" card. */
    val canSuggestMealIdeas: Boolean get() = consentGranted && mealIdeasEnabled

    /** "Prepare your consultation" and its "You could also ask" suggestions. */
    val canSuggestQuestions: Boolean get() = consentGranted && suggestedQuestionsEnabled

    /** "AI features · On": consent is granted and at least one feature is on. */
    val anyFeatureAvailable: Boolean get() = consentGranted && AiFeature.entries.any(::isEnabled)

    /** The saved preference for [feature] (ignoring consent). */
    fun isEnabled(feature: AiFeature): Boolean = when (feature) {
        AiFeature.WEEKLY_SUMMARY -> weeklySummaryEnabled
        AiFeature.MEAL_IDEAS -> mealIdeasEnabled
        AiFeature.SUGGESTED_QUESTIONS -> suggestedQuestionsEnabled
        AiFeature.MEAL_PHOTO_RECOGNITION -> mealPhotoRecognitionEnabled
    }

    /** The same preferences with [feature] turned on or off. */
    fun with(feature: AiFeature, enabled: Boolean): AiPreferences = when (feature) {
        AiFeature.WEEKLY_SUMMARY -> copy(weeklySummaryEnabled = enabled)
        AiFeature.MEAL_IDEAS -> copy(mealIdeasEnabled = enabled)
        AiFeature.SUGGESTED_QUESTIONS -> copy(suggestedQuestionsEnabled = enabled)
        AiFeature.MEAL_PHOTO_RECOGNITION -> copy(mealPhotoRecognitionEnabled = enabled)
    }

    companion object {
        /** No data (never read and no connection): no AI features. */
        val NoneGranted = AiPreferences(
            consentGranted = false,
            weeklySummaryEnabled = false,
            mealIdeasEnabled = false,
            suggestedQuestionsEnabled = false,
            mealPhotoRecognitionEnabled = false,
        )
    }
}
