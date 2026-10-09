package pe.edu.upc.healthify.features.monitoring

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetConsultationsOverviewUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetSuggestedQuestionsUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.OwnQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.QuestionOrigin
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.ConsultationsService
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.CheckInQuestionDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsultationCheckInSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsultationsOverviewDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PastConsultationDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PreVisitCheckInDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SubmitCheckInRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SuggestedQuestionDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SuggestedQuestionsDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.UpcomingConsultationDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.ConsultationsRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.presentation.state.CheckInCard
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.MyConsultationsViewModel
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.checkInCard
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeConsultationsRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.nextFollowUp
import pe.edu.upc.healthify.testing.suggestions
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant

/** PT25 · Mis consultas (RM-5), check-in (MA-4) y preguntas sugeridas (IA-4): dominio, mappers y repositorio. */
class ConsultationsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- Value objects y entidades ---

    @Test
    fun `an own question is trimmed and needs 3 to 300 characters`() {
        assertEquals(OwnQuestion.Empty, CheckInQuestion.ownQuestion("   "))
        assertEquals(OwnQuestion.TooShort, CheckInQuestion.ownQuestion(" ¿y "))
        assertEquals(OwnQuestion.TooLong, CheckInQuestion.ownQuestion("a".repeat(301)))
        val valid = CheckInQuestion.ownQuestion("  ¿Puedo comer fuera?  ") as OwnQuestion.Valid
        assertEquals("¿Puedo comer fuera?", valid.question.text)
        assertEquals(QuestionOrigin.PATIENT, valid.question.origin)
    }

    @Test
    fun `only an AI suggestion carries a language and an unknown language is dropped`() {
        assertThrows(IllegalArgumentException::class.java) {
            CheckInQuestion("¿Puedo comer fuera?", QuestionOrigin.PATIENT, language = "es")
        }
        assertEquals("en", CheckInQuestion.aiSuggestion("How do I plan?", 7, "EN")!!.language)
        assertNull(CheckInQuestion.aiSuggestion("¿Cómo armo cenas?", 7, "pt")!!.language)
    }

    @Test
    fun `a check in has at most 3 questions of each origin and no repeated one`() {
        val own = (1..4).map { CheckInQuestion("Pregunta $it", QuestionOrigin.PATIENT) }
        assertThrows(IllegalArgumentException::class.java) { CheckInAnswer(PlanFeeling.GOOD, questions = own) }
        val repeated = CheckInQuestion("Pregunta", QuestionOrigin.PATIENT)
        assertThrows(IllegalArgumentException::class.java) {
            CheckInAnswer(PlanFeeling.GOOD, questions = listOf(repeated, repeated))
        }
        val ai = (1..3).map { CheckInQuestion("Sugerencia $it", QuestionOrigin.AI_SUGGESTED) }
        assertEquals(6, CheckInAnswer(PlanFeeling.GOOD, questions = own.take(3) + ai).questions.size)
    }

    @Test
    fun `the check in card follows the hour of the consultation`() {
        val next = nextFollowUp(scheduledFor = Instant.parse("2026-09-18T15:00:00Z"))
        val before = Instant.parse("2026-09-18T14:59:00Z")
        val atTheHour = Instant.parse("2026-09-18T15:00:00Z")
        val unanswered = ConsultationsOverview(next, checkIn = null, past = emptyList())
        assertEquals(CheckInCard.Answer(31), unanswered.checkInCard(before))
        assertEquals(CheckInCard.Hidden, unanswered.checkInCard(atTheHour))

        val sent = CheckInSummary(PlanFeeling.FAIR, emptySet(), emptyList(), CONSULTATIONS_NOW, null, isLocked = false)
        val answered = unanswered.copy(checkIn = sent)
        assertTrue((answered.checkInCard(before) as CheckInCard.Sent).canEdit)
        assertEquals(false, (answered.checkInCard(atTheHour) as CheckInCard.Sent).canEdit)
        val locked = unanswered.copy(checkIn = sent.copy(isLocked = true))
        assertEquals(false, (locked.checkInCard(before) as CheckInCard.Sent).canEdit)
        assertEquals(CheckInCard.Hidden, ConsultationsOverview(null, null, emptyList()).checkInCard(before))
    }

    @Test
    fun `preparation codes are translated in the app and an unknown one is shown as is`() {
        assertEquals(PreparationInstruction.Catalog(PreparationCode.FASTING), PreparationInstruction.of("Fasting"))
        assertEquals(PreparationInstruction.Custom("BringGlucometer"), PreparationInstruction.of("BringGlucometer"))
        assertNull(PreparationInstruction.of(" "))
        assertEquals(ConsultationLabel.Custom("FollowUpOnly"), ConsultationLabel.of("FollowUpOnly"))
    }

    // --- Mappers ---

    @Test
    fun `the overview keeps the readable sections and drops what cannot be read`() {
        val overview = ConsultationsOverviewDto(
            patientId = 12,
            next = UpcomingConsultationDto(
                followUpId = 31,
                scheduledFor = "2026-09-18T10:00:00-05:00",
                modality = "Remote",
                preparation = listOf("Fasting", "LightClothing", "Fasting"),
                scheduledAt = "2026-09-04T09:00:00-05:00",
                practitionerFullName = "  Lucía Ramos ",
            ),
            checkIn = ConsultationCheckInSummaryDto(feeling = "Unknown", submittedAt = "2026-09-15T10:00:00-05:00"),
            past = listOf(
                PastConsultationDto(1, "2026-03-12T10:00:00-05:00", "FirstConsultation", 1),
                PastConsultationDto(2, "no es fecha", "AssessmentAndNewPlan", 2),
                PastConsultationDto(3, "2026-09-03T10:00:00-05:00", "AssessmentAndNewPlan", 3),
            ),
        ).toDomain()

        val next = overview.next!!
        assertEquals(ConsultationModality.REMOTE, next.modality)
        assertEquals(2, next.preparation.size)
        assertEquals("Lucía Ramos", next.practitionerFullName)
        assertEquals(Instant.parse("2026-09-04T14:00:00Z"), next.scheduledAt)
        // Un check-in ilegible queda vacío sin tumbar el resto.
        assertNull(overview.checkIn)
        assertEquals(listOf(3L, 1L), overview.past.map { it.id })
        assertEquals(ConsultationLabel.AssessmentAndNewPlan, overview.past.first().label)
    }

    @Test
    fun `a saved check in keeps the language of the accepted AI questions`() {
        val checkIn = PreVisitCheckInDto(
            followUpId = 31,
            feeling = "Hard",
            difficulties = listOf("Dinners", "Unknown"),
            questions = listOf(
                CheckInQuestionDto("¿Qué hago con los antojos?", "Patient"),
                CheckInQuestionDto("How do I plan dinners?", "AiSuggested", "en"),
                CheckInQuestionDto("no", "Patient"),
            ),
            submittedAt = "2026-09-14T10:00:00-05:00",
            isLocked = true,
        ).toDomainOrNull()!!

        assertEquals(PlanFeeling.HARD, checkIn.answer.feeling)
        assertEquals(setOf(CheckInDifficulty.DINNERS), checkIn.answer.difficulties)
        assertEquals(listOf("¿Qué hago con los antojos?"), checkIn.answer.ownQuestions.map { it.text })
        assertEquals("en", checkIn.answer.aiQuestions.single().language)
        assertTrue(checkIn.isLocked)
    }

    @Test
    fun `the answer is sent with codes, its origin, the generation and the language`() {
        val dto = CheckInAnswer(
            feeling = PlanFeeling.FAIR,
            difficulties = setOf(CheckInDifficulty.WEEKENDS, CheckInDifficulty.DINNERS),
            questions = listOf(
                CheckInQuestion("¿Puedo comer fuera?", QuestionOrigin.PATIENT),
                CheckInQuestion("¿Cómo armo cenas?", QuestionOrigin.AI_SUGGESTED, aiGenerationId = 77, language = "es"),
            ),
        ).toDto()

        assertEquals("Fair", dto.feeling)
        assertEquals(listOf("Dinners", "Weekends"), dto.difficulties)
        assertEquals(listOf("Patient", "AiSuggested"), dto.questions.map { it.origin })
        assertNull(dto.questions[0].language)
        assertEquals(77L, dto.questions[1].aiGenerationId)
        assertEquals("es", dto.questions[1].language)
    }

    // --- Repositorio ---

    @Test
    fun `404 PreVisitCheckInNotFound means not answered yet but another 404 is an error`() = runTest {
        val service = FakeConsultationsService()
        val repository = ConsultationsRepositoryImpl(service)

        service.failWith = http(404, "PreVisitCheckInNotFound")
        assertNull(repository.getCheckIn(FollowUpId(31)).getOrThrow())

        service.failWith = http(404, "ScheduledFollowUpNotFound")
        assertEquals(DomainError.NotFound("ScheduledFollowUpNotFound"), repository.getCheckIn(FollowUpId(31)).domainErrorOrNull())
    }

    @Test
    fun `suggested questions not yet, switched off or failing`() = runTest {
        val service = FakeConsultationsService()
        val repository = ConsultationsRepositoryImpl(service)
        val ready = repository.getSuggestedQuestions(PATIENT, FollowUpId(31)).getOrThrow() as SuggestedQuestionsAvailability.Ready
        assertEquals("es", ready.suggestions.language)
        assertEquals(31L, service.requestedFollowUp)

        service.failWith = http(404, "NotEnoughData")
        assertEquals(SuggestedQuestionsAvailability.NotYet, repository.getSuggestedQuestions(PATIENT, null).getOrThrow())
        service.failWith = HttpException(Response.error<Any>(403, "".toResponseBody()))
        assertEquals(SuggestedQuestionsAvailability.Off, repository.getSuggestedQuestions(PATIENT, null).getOrThrow())
        service.failWith = http(503, "AiFeatureDisabled")
        assertEquals(SuggestedQuestionsAvailability.Off, repository.getSuggestedQuestions(PATIENT, null).getOrThrow())
        service.failWith = IOException("offline")
        assertEquals(DomainError.Network, repository.getSuggestedQuestions(PATIENT, null).domainErrorOrNull())
    }

    @Test
    fun `409 CheckInLocked is returned as a conflict with its code`() = runTest {
        val service = FakeConsultationsService().apply { failWith = http(409, "CheckInLocked") }

        val result = ConsultationsRepositoryImpl(service).submitCheckIn(FollowUpId(31), CheckInAnswer(PlanFeeling.GOOD))

        assertEquals(DomainError.Conflict("CheckInLocked"), result.domainErrorOrNull())
    }

    // --- PT25 ViewModel ---

    @Test
    fun `PT25 shows the next consultation, the answer card and the AI questions only when turned on`() = runTest {
        val consultations = FakeConsultationsRepository().apply {
            overviewResult = Result.success(ConsultationsOverview(nextFollowUp(), checkIn = null, past = emptyList()))
            suggestionsResult = Result.success(SuggestedQuestionsAvailability.Ready(suggestions("¿Cómo armo cenas?")))
        }
        val preferences = FakeAiPreferencesRepository(aiPreferences(consent = true))
        val viewModel = myConsultations(consultations, preferences)

        val state = viewModel.state.value
        assertEquals(31L, state.next!!.id.value)
        assertEquals(CheckInCard.Answer(31), state.checkInCard)
        assertEquals(listOf("¿Cómo armo cenas?"), state.suggestedQuestions.map { it.text })
        assertEquals(listOf(FollowUpId(31)), consultations.suggestionRequests)

        // Al volver de PT25.2 se relee, pero las preguntas no se vuelven a pedir si el check-in no cambió.
        viewModel.onResume()
        assertEquals(1, consultations.suggestionRequests.size)

        val off = FakeConsultationsRepository().apply { overviewResult = consultations.overviewResult }
        val offViewModel = myConsultations(off, FakeAiPreferencesRepository(aiPreferences(consent = false)))
        assertTrue(offViewModel.state.value.suggestedQuestions.isEmpty())
        assertTrue(off.suggestionRequests.isEmpty())
    }

    @Test
    fun `PT25 without network and nothing loaded shows the offline state`() = runTest {
        val consultations = FakeConsultationsRepository().apply { overviewResult = failureOf(DomainError.Network) }

        val state = myConsultations(consultations, FakeAiPreferencesRepository()).state.value

        assertTrue(state.loadFailed)
        assertTrue(state.isOffline)
    }

    private fun myConsultations(consultations: FakeConsultationsRepository, preferences: FakeAiPreferencesRepository) =
        MyConsultationsViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
            getConsultationsOverview = GetConsultationsOverviewUseCase(consultations),
            getSuggestedQuestions = GetSuggestedQuestionsUseCase(consultations),
            getAiPreferences = GetAiPreferencesUseCase(preferences),
            connectivityObserver = FakeConnectivityObserver(),
            clock = fixedClock(),
        )

    private class FakeConsultationsService : ConsultationsService {
        var failWith: Exception? = null
        var requestedFollowUp: Long? = null

        override suspend fun getOverview(patientId: Long): ConsultationsOverviewDto {
            failWith?.let { throw it }
            return ConsultationsOverviewDto(patientId)
        }

        override suspend fun getCheckIn(followUpId: Long): PreVisitCheckInDto {
            failWith?.let { throw it }
            return PreVisitCheckInDto(followUpId, "Good", submittedAt = "2026-09-14T10:00:00-05:00")
        }

        override suspend fun submitCheckIn(followUpId: Long, body: SubmitCheckInRequestDto): PreVisitCheckInDto {
            failWith?.let { throw it }
            return PreVisitCheckInDto(followUpId, body.feeling, submittedAt = "2026-09-15T10:00:00-05:00")
        }

        override suspend fun getSuggestedQuestions(patientId: Long, followUpId: Long?): SuggestedQuestionsDto {
            requestedFollowUp = followUpId
            failWith?.let { throw it }
            return SuggestedQuestionsDto(listOf(SuggestedQuestionDto("q1", "¿Cómo armo cenas?")), aiGenerationId = 9, language = "es")
        }
    }

    private companion object {
        val PATIENT = PatientId(12)

        fun http(status: Int, code: String) = HttpException(
            Response.error<Any>(status, """{"status":$status,"code":"$code"}""".toResponseBody()),
        )
    }
}
