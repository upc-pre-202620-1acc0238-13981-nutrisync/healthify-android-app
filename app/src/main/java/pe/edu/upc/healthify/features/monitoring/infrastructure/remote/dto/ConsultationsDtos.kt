package pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `PatientConsultationsOverviewResource` (RM-5). */
@Serializable
data class ConsultationsOverviewDto(
    val patientId: Long,
    val next: UpcomingConsultationDto? = null,
    val checkIn: ConsultationCheckInSummaryDto? = null,
    val past: List<PastConsultationDto> = emptyList(),
)

@Serializable
data class UpcomingConsultationDto(
    val followUpId: Long,
    val scheduledFor: String,
    val modality: String? = null,
    val preparation: List<String> = emptyList(),
    val scheduledAt: String? = null,
    val practitionerFullName: String? = null,
)

@Serializable
data class ConsultationCheckInSummaryDto(
    val feeling: String,
    val difficulties: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val submittedAt: String,
    val editedAt: String? = null,
    val isLocked: Boolean = false,
)

@Serializable
data class PastConsultationDto(
    val consultationId: Long,
    val date: String,
    val label: String? = null,
    val planVersion: Int? = null,
)

/** `PreVisitCheckInResource` (MA-4). */
@Serializable
data class PreVisitCheckInDto(
    val followUpId: Long,
    val feeling: String,
    val difficulties: List<String> = emptyList(),
    val questions: List<CheckInQuestionDto> = emptyList(),
    val submittedAt: String,
    val editedAt: String? = null,
    val isLocked: Boolean = false,
)

@Serializable
data class CheckInQuestionDto(
    val text: String,
    val origin: String? = null,
    val language: String? = null,
)

/** `SubmitPreVisitCheckInResource`. */
@Serializable
data class SubmitCheckInRequestDto(
    val feeling: String,
    val difficulties: List<String>,
    val questions: List<CheckInQuestionInputDto>,
)

/** `CheckInQuestionInputResource`: `aiGenerationId` y `language` solo para `AiSuggested`. */
@Serializable
data class CheckInQuestionInputDto(
    val text: String,
    val origin: String,
    val aiGenerationId: Long? = null,
    val language: String? = null,
)

/** `SuggestedQuestionsResource` (IA-4). */
@Serializable
data class SuggestedQuestionsDto(
    val questions: List<SuggestedQuestionDto> = emptyList(),
    val aiGenerationId: Long,
    val basedOnFrom: String? = null,
    val basedOnTo: String? = null,
    val language: String? = null,
)

@Serializable
data class SuggestedQuestionDto(
    val id: String,
    val text: String,
)
