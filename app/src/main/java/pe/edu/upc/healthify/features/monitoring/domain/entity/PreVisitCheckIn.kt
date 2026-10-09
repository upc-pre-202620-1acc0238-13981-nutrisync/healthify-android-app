package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.QuestionOrigin
import java.time.Instant

/**
 * «Cuéntale cómo te fue» tal como lo guardó el backend (`PreVisitCheckInResource`, MA-4). Uno por consulta; el
 * paciente lo edita hasta la hora de la consulta ([isLocked] lo dice el backend). Es voluntario y no levanta señales.
 */
data class PreVisitCheckIn(
    val followUpId: FollowUpId,
    val answer: CheckInAnswer,
    val submittedAt: Instant,
    val editedAt: Instant?,
    val isLocked: Boolean,
)

/**
 * Lo que el paciente responde (`SubmitPreVisitCheckInResource`): cómo se sintió (obligatorio), dificultades (sin
 * repetir) y hasta 3 preguntas propias y 3 sugerencias de IA aceptadas.
 */
data class CheckInAnswer(
    val feeling: PlanFeeling,
    val difficulties: Set<CheckInDifficulty> = emptySet(),
    val questions: List<CheckInQuestion> = emptyList(),
) {
    init {
        require(questions.distinct().size == questions.size) { "A check in does not repeat a question" }
        require(questions.groupBy { it.origin }.values.all { it.size <= MAX_QUESTIONS_PER_ORIGIN }) {
            "A check in has at most $MAX_QUESTIONS_PER_ORIGIN questions of each origin"
        }
    }

    val ownQuestions: List<CheckInQuestion> get() = questions.filter { it.origin == QuestionOrigin.PATIENT }
    val aiQuestions: List<CheckInQuestion> get() = questions.filter { it.origin == QuestionOrigin.AI_SUGGESTED }

    companion object {
        const val MAX_QUESTIONS_PER_ORIGIN = 3
    }
}

/** Resumen del check-in dentro de Mis consultas (`ConsultationCheckInSummaryResource`): las preguntas solo como texto. */
data class CheckInSummary(
    val feeling: PlanFeeling,
    val difficulties: Set<CheckInDifficulty>,
    val questions: List<String>,
    val submittedAt: Instant,
    val editedAt: Instant?,
    val isLocked: Boolean,
)

/** Preguntas sugeridas por IA para llevar a la consulta (`SuggestedQuestionsResource`, IA-4). */
data class SuggestedQuestions(
    val questions: List<SuggestedQuestion>,
    val aiGenerationId: Long,
    /** Idioma en que se generaron (X-2); se manda con cada sugerencia aceptada. */
    val language: String?,
)

data class SuggestedQuestion(val id: String, val text: String) {
    init {
        require(text.isNotBlank()) { "A suggested question needs text" }
    }
}

/** IA-4: las preguntas, «aún no» (`404 NotEnoughData`) o la función apagada (`403`/`503 AiFeatureDisabled`). */
sealed interface SuggestedQuestionsAvailability {
    data class Ready(val suggestions: SuggestedQuestions) : SuggestedQuestionsAvailability
    data object NotYet : SuggestedQuestionsAvailability
    data object Off : SuggestedQuestionsAvailability
}
