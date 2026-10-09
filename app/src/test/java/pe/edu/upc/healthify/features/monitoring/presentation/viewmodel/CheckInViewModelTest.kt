package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetCheckInUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetSuggestedQuestionsUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.SubmitCheckInUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.QuestionOrigin
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.CheckInRoute
import pe.edu.upc.healthify.features.monitoring.presentation.state.OwnQuestionError
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeConsultationsRepository
import pe.edu.upc.healthify.testing.FakePatientMonitoringRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.nextFollowUp
import pe.edu.upc.healthify.testing.suggestions
import java.time.Instant

/** PT25.2 · Cuéntale cómo te fue: envío, edición de la respuesta guardada y bloqueo a la hora de la consulta (MA-4). */
class CheckInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val consultations = FakeConsultationsRepository()
    private val monitoring = FakePatientMonitoringRepository().apply { nextFollowUpResult = Result.success(nextFollowUp()) }
    private val aiPreferences = FakeAiPreferencesRepository(aiPreferences(consent = true))
    private val connectivity = FakeConnectivityObserver()
    private var now: Instant = CONSULTATIONS_NOW

    private fun viewModel(followUpId: Long = 31) = CheckInViewModel(
        savedStateHandle = SavedStateHandle(mapOf(CheckInRoute.ARG_FOLLOW_UP_ID to followUpId)),
        observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
        getNextFollowUp = GetNextFollowUpUseCase(monitoring),
        getCheckIn = GetCheckInUseCase(consultations),
        submitCheckIn = SubmitCheckInUseCase(consultations),
        getSuggestedQuestions = GetSuggestedQuestionsUseCase(consultations),
        getAiPreferences = GetAiPreferencesUseCase(aiPreferences),
        connectivityObserver = connectivity,
        clock = fixedClock(now),
    )

    private fun saved(
        answer: CheckInAnswer,
        isLocked: Boolean = false,
    ) = PreVisitCheckIn(FollowUpId(31), answer, submittedAt = Instant.parse("2026-09-14T15:00:00Z"), editedAt = null, isLocked = isLocked)

    @Test
    fun `a new answer is sent with the feeling, the difficulties, the own question and the AI suggestion with its language`() = runTest {
        consultations.suggestionsResult = Result.success(
            SuggestedQuestionsAvailability.Ready(suggestions("¿Cómo armo cenas con más proteína?", "¿Puedo ajustar el plan?")),
        )
        val vm = viewModel()
        assertFalse(vm.state.value.isEditing)
        assertEquals(2, vm.state.value.suggestions.size)

        vm.onFeelingSelected(PlanFeeling.FAIR)
        vm.onDifficultyToggled(CheckInDifficulty.DINNERS, true)
        vm.onDifficultyToggled(CheckInDifficulty.WEEKENDS, true)
        vm.onOwnQuestionChanged("  ¿Puedo comer fuera los viernes?  ")
        vm.onSuggestionToggled("¿Cómo armo cenas con más proteína?", true)

        vm.events.test {
            vm.onSubmit()
            assertEquals(CheckInEvent.Submitted, awaitItem())
        }
        val (followUpId, answer) = consultations.submitted.single()
        assertEquals(31L, followUpId.value)
        assertEquals(PlanFeeling.FAIR, answer.feeling)
        assertEquals(setOf(CheckInDifficulty.DINNERS, CheckInDifficulty.WEEKENDS), answer.difficulties)
        assertEquals(listOf("¿Puedo comer fuera los viernes?"), answer.ownQuestions.map { it.text })
        val ai = answer.aiQuestions.single()
        assertEquals("¿Cómo armo cenas con más proteína?", ai.text)
        assertEquals(77L, ai.aiGenerationId)
        assertEquals("es", ai.language)
    }

    @Test
    fun `the feeling is required and a question shorter than 3 characters is not sent`() = runTest {
        val vm = viewModel()

        vm.onOwnQuestionChanged("¿y")
        vm.onSubmit()

        assertTrue(vm.state.value.feelingMissing)
        assertEquals(OwnQuestionError.TOO_SHORT, vm.state.value.ownQuestionError)
        assertTrue(consultations.submitted.isEmpty())
    }

    @Test
    fun `editing prefills the saved answer and keeps the accepted AI question with its language`() = runTest {
        consultations.checkInResult = Result.success(
            saved(
                CheckInAnswer(
                    feeling = PlanFeeling.HARD,
                    difficulties = setOf(CheckInDifficulty.CRAVINGS),
                    questions = listOf(
                        CheckInQuestion("¿Qué hago con los antojos?", QuestionOrigin.PATIENT),
                        CheckInQuestion("How do I plan dinners?", QuestionOrigin.AI_SUGGESTED, language = "en"),
                    ),
                ),
            ),
        )
        consultations.suggestionsResult = Result.success(
            SuggestedQuestionsAvailability.Ready(suggestions("How do I plan dinners?", "¿Puedo ajustar el plan?")),
        )
        val vm = viewModel()

        val state = vm.state.value
        assertTrue(state.isEditing)
        assertEquals(PlanFeeling.HARD, state.feeling)
        assertEquals(setOf(CheckInDifficulty.CRAVINGS), state.difficulties)
        assertEquals("¿Qué hago con los antojos?", state.ownQuestion)
        // La aceptada no se duplica con la sugerencia nueva del mismo texto.
        assertEquals(listOf("How do I plan dinners?", "¿Puedo ajustar el plan?"), state.suggestions.map { it.text })
        assertEquals(listOf(true, false), state.suggestions.map { it.selected })

        vm.onFeelingSelected(PlanFeeling.GOOD)
        vm.onSubmit()

        val answer = consultations.submitted.single().second
        assertEquals(PlanFeeling.GOOD, answer.feeling)
        assertEquals("en", answer.aiQuestions.single().language)
        assertEquals("¿Qué hago con los antojos?", answer.ownQuestions.single().text)
    }

    @Test
    fun `from the hour of the consultation the check in is locked and nothing is sent`() = runTest {
        now = Instant.parse("2026-09-18T15:00:00Z")
        val vm = viewModel()

        assertTrue(vm.state.value.isLocked)
        assertFalse(vm.state.value.canSubmit)
        vm.onFeelingSelected(PlanFeeling.GOOD)
        vm.onSubmit()

        assertTrue(consultations.submitted.isEmpty())
        // Bloqueado: ni siquiera se piden sugerencias (cuentan para la cuota).
        assertTrue(consultations.suggestionRequests.isEmpty())
    }

    @Test
    fun `a saved answer the backend reports as locked cannot be edited`() = runTest {
        consultations.checkInResult = Result.success(saved(CheckInAnswer(PlanFeeling.FAIR), isLocked = true))

        val vm = viewModel()

        assertTrue(vm.state.value.isEditing)
        assertTrue(vm.state.value.isLocked)
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun `409 CheckInLocked while sending locks the screen instead of showing the error`() = runTest {
        consultations.submitResults = listOf(failureOf(DomainError.Conflict("CheckInLocked")))
        val vm = viewModel()

        vm.onFeelingSelected(PlanFeeling.GOOD)
        vm.onSubmit()

        assertTrue(vm.state.value.isLocked)
        assertFalse(vm.state.value.submitFailed)
    }

    @Test
    fun `without network PT25_2_E keeps what was written and retrying sends it again`() = runTest {
        consultations.submitResults = listOf(failureOf(DomainError.Network), Result.success(saved(CheckInAnswer(PlanFeeling.GOOD))))
        val vm = viewModel()
        vm.onFeelingSelected(PlanFeeling.GOOD)
        vm.onOwnQuestionChanged("¿Puedo comer fuera?")

        vm.onSubmit()
        assertTrue(vm.state.value.submitFailed)
        assertEquals("¿Puedo comer fuera?", vm.state.value.ownQuestion)

        vm.events.test {
            vm.onRetrySubmit()
            assertEquals(CheckInEvent.Submitted, awaitItem())
        }
        assertEquals(2, consultations.submitted.size)
        assertEquals(consultations.submitted[0].second, consultations.submitted[1].second)
    }

    @Test
    fun `at most 3 AI suggestions can be selected`() = runTest {
        consultations.suggestionsResult = Result.success(SuggestedQuestionsAvailability.Ready(suggestions("Uno?", "Dos?", "Tres?", "Cuatro?")))
        val vm = viewModel()

        listOf("Uno?", "Dos?", "Tres?", "Cuatro?").forEach { vm.onSuggestionToggled(it, true) }

        assertEquals(listOf(true, true, true, false), vm.state.value.suggestions.map { it.selected })
        assertFalse(vm.state.value.canSelectMoreSuggestions)
    }

    @Test
    fun `a consultation that is no longer the next scheduled one is not answered`() = runTest {
        monitoring.nextFollowUpResult = Result.success(nextFollowUp(id = 40))

        val vm = viewModel(followUpId = 31)

        assertTrue(vm.state.value.notScheduled)
        assertNull(consultations.submitted.firstOrNull())
    }

    @Test
    fun `with suggested questions turned off they are not requested`() = runTest {
        aiPreferences.preferences = aiPreferences(consent = false)

        val vm = viewModel()

        assertTrue(vm.state.value.suggestions.isEmpty())
        assertTrue(consultations.suggestionRequests.isEmpty())
    }
}
