package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PastConsultation
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestion
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestions
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.OwnQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.QuestionOrigin
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.CheckInQuestionDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.CheckInQuestionInputDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsultationCheckInSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsultationsOverviewDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PastConsultationDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PreVisitCheckInDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SubmitCheckInRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SuggestedQuestionsDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.UpcomingConsultationDto

/**
 * El read model es tolerante (una sección que falla queda vacía): una próxima consulta, un check-in o una
 * consulta anterior que no se puede leer se omite en vez de tumbar toda la pantalla.
 */
fun ConsultationsOverviewDto.toDomain(): ConsultationsOverview = ConsultationsOverview(
    next = next?.toDomainOrNull(),
    checkIn = checkIn?.toDomainOrNull(),
    past = past.mapNotNull { it.toDomainOrNull() }.sortedByDescending { it.date },
)

fun UpcomingConsultationDto.toDomainOrNull() = try {
    nextFollowUpOf(followUpId, scheduledFor, modality, preparation, scheduledAt, practitionerFullName)
} catch (_: IllegalArgumentException) {
    null
}

fun ConsultationCheckInSummaryDto.toDomainOrNull(): CheckInSummary? {
    val feeling = PlanFeeling.fromCode(feeling) ?: return null
    val submitted = submittedAt.toInstantOrNull() ?: return null
    return CheckInSummary(
        feeling = feeling,
        difficulties = difficulties.mapNotNull(CheckInDifficulty::fromCode).toSet(),
        questions = questions.map(String::trim).filter(String::isNotEmpty),
        submittedAt = submitted,
        editedAt = editedAt?.toInstantOrNull(),
        isLocked = isLocked,
    )
}

fun PastConsultationDto.toDomainOrNull(): PastConsultation? {
    val date = date.toInstantOrNull() ?: return null
    return PastConsultation(
        id = consultationId,
        date = date,
        label = ConsultationLabel.of(label),
        planVersion = planVersion?.takeIf { it > 0 },
    )
}

/** `null` si el recurso no se puede leer (sentimiento desconocido, fecha ilegible). Una pregunta inválida se omite. */
fun PreVisitCheckInDto.toDomainOrNull(): PreVisitCheckIn? {
    val feeling = PlanFeeling.fromCode(feeling) ?: return null
    val submitted = submittedAt.toInstantOrNull() ?: return null
    return try {
        PreVisitCheckIn(
            followUpId = FollowUpId(followUpId),
            answer = CheckInAnswer(
                feeling = feeling,
                difficulties = difficulties.mapNotNull(CheckInDifficulty::fromCode).toSet(),
                questions = questions.mapNotNull { it.toDomainOrNull() }.distinct(),
            ),
            submittedAt = submitted,
            editedAt = editedAt?.toInstantOrNull(),
            isLocked = isLocked,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** El recurso de lectura no trae `aiGenerationId`: una sugerencia aceptada vuelve solo con su texto e idioma. */
fun CheckInQuestionDto.toDomainOrNull(): CheckInQuestion? = when (QuestionOrigin.fromCode(origin)) {
    QuestionOrigin.AI_SUGGESTED -> CheckInQuestion.aiSuggestion(text, aiGenerationId = null, language = language)
    QuestionOrigin.PATIENT -> (CheckInQuestion.ownQuestion(text) as? OwnQuestion.Valid)?.question
}

fun CheckInAnswer.toDto(): SubmitCheckInRequestDto = SubmitCheckInRequestDto(
    feeling = feeling.code,
    difficulties = CheckInDifficulty.entries.filter { it in difficulties }.map { it.code },
    questions = questions.map { question ->
        CheckInQuestionInputDto(
            text = question.text,
            origin = question.origin.code,
            aiGenerationId = question.aiGenerationId,
            language = question.language,
        )
    },
)

fun SuggestedQuestionsDto.toDomain(): SuggestedQuestions = SuggestedQuestions(
    questions = questions
        .filter { it.text.isNotBlank() }
        .map { SuggestedQuestion(id = it.id, text = it.text.trim()) }
        .distinctBy { it.text },
    aiGenerationId = aiGenerationId,
    language = language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
)
