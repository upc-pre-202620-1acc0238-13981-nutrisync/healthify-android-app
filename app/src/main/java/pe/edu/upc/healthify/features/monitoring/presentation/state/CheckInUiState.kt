package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling

/** PT25.2 · Cuéntale cómo te fue (+ PT25.2.E no se pudo enviar). */
data class CheckInUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    /** Ya había una respuesta: se está editando (PT25.3 «Editar mi respuesta»). */
    val isEditing: Boolean = false,
    val feeling: PlanFeeling? = null,
    /** «Elige cómo te sentiste» (el sentimiento es obligatorio, MA-4). */
    val feelingMissing: Boolean = false,
    val difficulties: Set<CheckInDifficulty> = emptySet(),
    /** La pregunta libre del paciente, tal cual la escribe (nunca se traduce). */
    val ownQuestion: String = "",
    val ownQuestionError: OwnQuestionError? = null,
    /** «También podrías preguntar» (IA-4): vacío = la card no aparece. */
    val suggestions: List<SuggestionOption> = emptyList(),
    val isSubmitting: Boolean = false,
    /** PT25.2.E: red o servidor; lo escrito se conserva. */
    val submitFailed: Boolean = false,
    /** Llegó la hora de la consulta (o el backend lo bloqueó): ya no se puede enviar ni editar. */
    val isLocked: Boolean = false,
    /** La consulta ya no está agendada (movida o cancelada). */
    val notScheduled: Boolean = false,
) {
    /** Hasta 3 sugerencias de IA por check-in (MA-4): con 3 elegidas, el resto no se puede marcar. */
    val canSelectMoreSuggestions: Boolean get() = suggestions.count { it.selected } < MAX_AI_QUESTIONS

    val canSubmit: Boolean get() = !isLoading && !loadFailed && !isSubmitting && !isLocked && !notScheduled

    companion object {
        const val MAX_AI_QUESTIONS = 3
        const val MAX_QUESTION_LENGTH = 300
    }
}

enum class OwnQuestionError { TOO_SHORT, TOO_LONG }

/** Una sugerencia de IA: se agrega con su idioma y la generación de la que salió. */
data class SuggestionOption(
    val text: String,
    val selected: Boolean,
    val aiGenerationId: Long?,
    val language: String?,
)
