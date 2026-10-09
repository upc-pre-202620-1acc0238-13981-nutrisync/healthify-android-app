package pe.edu.upc.healthify.features.carerelationship.domain.entity

/**
 * Consentimiento de IA y preferencias por función del paciente (`AiPreferencesResource`, CR-2/IA-1/IN-7). Una
 * función con IA solo está disponible si el consentimiento de IA sigue vigente **y** su preferencia está encendida.
 * Que la IA esté apagada en el servidor (`503 AiFeatureDisabled`) no se sabe por aquí: lo dice cada función.
 */
/** Las funciones con IA que el paciente enciende o apaga en PT21.IA (IA-1, IN-7). */
enum class AiFeature { WEEKLY_SUMMARY, MEAL_IDEAS, SUGGESTED_QUESTIONS, MEAL_PHOTO_RECOGNITION }

data class AiPreferences(
    val consentGranted: Boolean,
    val weeklySummaryEnabled: Boolean,
    val mealIdeasEnabled: Boolean,
    val suggestedQuestionsEnabled: Boolean,
    val mealPhotoRecognitionEnabled: Boolean,
) {
    /** PT5: «Registrar comida» abre la cámara; si no, va directo a registrar a mano (PT9). */
    val canRecognizeMealPhotos: Boolean get() = consentGranted && mealPhotoRecognitionEnabled

    /** PT14: la card «¿No sabes qué comer?» (IA-3). */
    val canSuggestMealIdeas: Boolean get() = consentGranted && mealIdeasEnabled

    /** PT25 «Prepara tu consulta» y PT25.2 «También podrías preguntar» (IA-4). */
    val canSuggestQuestions: Boolean get() = consentGranted && suggestedQuestionsEnabled

    /** PT21 «Funciones con IA · Activadas»: hay consentimiento y al menos una función encendida. */
    val anyFeatureAvailable: Boolean get() = consentGranted && AiFeature.entries.any(::isEnabled)

    /** La preferencia guardada de [feature] (sin mirar el consentimiento). */
    fun isEnabled(feature: AiFeature): Boolean = when (feature) {
        AiFeature.WEEKLY_SUMMARY -> weeklySummaryEnabled
        AiFeature.MEAL_IDEAS -> mealIdeasEnabled
        AiFeature.SUGGESTED_QUESTIONS -> suggestedQuestionsEnabled
        AiFeature.MEAL_PHOTO_RECOGNITION -> mealPhotoRecognitionEnabled
    }

    /** Las mismas preferencias con [feature] encendida o apagada. */
    fun with(feature: AiFeature, enabled: Boolean): AiPreferences = when (feature) {
        AiFeature.WEEKLY_SUMMARY -> copy(weeklySummaryEnabled = enabled)
        AiFeature.MEAL_IDEAS -> copy(mealIdeasEnabled = enabled)
        AiFeature.SUGGESTED_QUESTIONS -> copy(suggestedQuestionsEnabled = enabled)
        AiFeature.MEAL_PHOTO_RECOGNITION -> copy(mealPhotoRecognitionEnabled = enabled)
    }

    companion object {
        /** Sin datos (nunca se leyeron y no hay conexión): ninguna función con IA. */
        val NoneGranted = AiPreferences(
            consentGranted = false,
            weeklySummaryEnabled = false,
            mealIdeasEnabled = false,
            suggestedQuestionsEnabled = false,
            mealPhotoRecognitionEnabled = false,
        )
    }
}
