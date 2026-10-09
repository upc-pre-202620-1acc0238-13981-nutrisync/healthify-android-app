package pe.edu.upc.healthify.testing

import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestion
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestions
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationsRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** «Ahora» de los tests de consultas: 15 sept. 2026, 10:00 (Lima, UTC−5). */
val CONSULTATIONS_NOW: Instant = Instant.parse("2026-09-15T15:00:00Z")

fun fixedClock(now: Instant = CONSULTATIONS_NOW): Clock = Clock.fixed(now, ZoneOffset.ofHours(-5))

/** La consulta del 18 sept. a las 10:00 (después de [CONSULTATIONS_NOW]). */
fun nextFollowUp(id: Long = 31, scheduledFor: Instant = Instant.parse("2026-09-18T15:00:00Z")) =
    NextFollowUp(id = FollowUpId(id), scheduledFor = scheduledFor)

fun suggestions(vararg texts: String, generation: Long = 77, language: String? = "es") = SuggestedQuestions(
    questions = texts.mapIndexed { index, text -> SuggestedQuestion("q$index", text) },
    aiGenerationId = generation,
    language = language,
)

/** Respuestas programables; registra cada envío y cada pedido de sugerencias. */
class FakeConsultationsRepository : ConsultationsRepository {
    var overviewResult: Result<ConsultationsOverview> =
        Result.success(ConsultationsOverview(next = null, checkIn = null, past = emptyList()))
    var checkInResult: Result<PreVisitCheckIn?> = Result.success(null)

    /** `null` = acepta y devuelve lo enviado. */
    var submitResults: List<Result<PreVisitCheckIn>>? = null
    var suggestionsResult: Result<SuggestedQuestionsAvailability> = Result.success(SuggestedQuestionsAvailability.NotYet)

    val submitted = mutableListOf<Pair<FollowUpId, CheckInAnswer>>()
    val suggestionRequests = mutableListOf<FollowUpId?>()

    override suspend fun getOverview(patientId: PatientId) = overviewResult

    override suspend fun getCheckIn(followUpId: FollowUpId) = checkInResult

    override suspend fun submitCheckIn(followUpId: FollowUpId, answer: CheckInAnswer): Result<PreVisitCheckIn> {
        submitted += followUpId to answer
        val results = submitResults
        if (results != null) return results[minOf(submitted.lastIndex, results.lastIndex)]
        return Result.success(PreVisitCheckIn(followUpId, answer, CONSULTATIONS_NOW, editedAt = null, isLocked = false))
    }

    override suspend fun getSuggestedQuestions(
        patientId: PatientId,
        followUpId: FollowUpId?,
    ): Result<SuggestedQuestionsAvailability> {
        suggestionRequests += followUpId
        return suggestionsResult
    }
}
