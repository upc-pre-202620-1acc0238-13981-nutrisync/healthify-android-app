package pe.edu.upc.healthify.features.intake.presentation.viewmodel

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
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.AnalyzeMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.DiscardMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetPendingMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.KeepPhotoMealUnconfirmedUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogPhotoMealUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.SavePendingMealPhotoUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.PendingMealPhoto
import pe.edu.upc.healthify.features.intake.domain.entity.PhotoConfirmation
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.presentation.navigation.MealPhotoRoute
import pe.edu.upc.healthify.features.intake.presentation.state.AnalysisFailure
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealPhotoStep
import pe.edu.upc.healthify.testing.FakeAiPreferencesRepository
import pe.edu.upc.healthify.testing.FakeAccountRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeDiaryRepository
import pe.edu.upc.healthify.testing.FakeMealPhotoRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.aiPreferences
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import java.time.OffsetDateTime

class MealPhotoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeAiPreferencesRepository()
    private val photos = FakeMealPhotoRepository()
    private val diary = FakeDiaryRepository()
    private val connectivity = FakeConnectivityObserver()
    private val savedStateHandle = SavedStateHandle()

    private val viewModel by lazy {
        MealPhotoViewModel(
            savedStateHandle,
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetAccountCreatedOnUseCase(FakeAccountRepository()),
            GetAiPreferencesUseCase(preferences),
            AnalyzeMealPhotoUseCase(photos),
            SavePendingMealPhotoUseCase(photos),
            GetPendingMealPhotoUseCase(photos),
            DiscardMealPhotoUseCase(photos),
            LogPhotoMealUseCase(diary),
            KeepPhotoMealUnconfirmedUseCase(diary),
            connectivity,
            fixedClock,
        )
    }

    /** PT5 → PT6 → PT6.1 → PT7. */
    private fun takeAndUsePhoto() {
        viewModel.onCameraPermissionResult(true)
        viewModel.onPhotoCaptured(PHOTO)
        viewModel.onUsePhoto()
    }

    @Test
    fun `with AI on, the flow starts at the camera`() {
        assertEquals(MealPhotoStep.CAMERA, viewModel.state.value.step)
    }

    @Test
    fun `without AI consent the photo button goes straight to logging by hand`() = runTest {
        preferences.preferences = aiPreferences(consent = false)

        viewModel.events.test { assertEquals(MealPhotoEvent.OpenManual, awaitItem()) }
        assertEquals(MealPhotoStep.RESOLVING, viewModel.state.value.step)
    }

    @Test
    fun `with «Reconocer comidas por foto» off it goes to logging by hand too`() = runTest {
        preferences.preferences = aiPreferences(photo = false)

        viewModel.events.test { assertEquals(MealPhotoEvent.OpenManual, awaitItem()) }
    }

    @Test
    fun `success - the proposal is confirmed as proposed with its analysis and the photo is deleted`() = runTest {
        takeAndUsePhoto()
        assertEquals(MealPhotoStep.PROPOSAL, viewModel.state.value.step)
        assertEquals("320", viewModel.state.value.form.portionText)

        viewModel.onPlanAnswer(true)
        viewModel.onConfirmProposal()

        viewModel.events.test { assertEquals(MealPhotoEvent.Logged(MealLogOutcome.LOGGED), awaitItem()) }
        val meal = diary.photoMeals.single()
        assertEquals(PhotoConfirmation.AsProposed, meal.confirmation)
        assertEquals(PlanAdherence.IN_PLAN, meal.planAdherence)
        assertEquals("8f14e45f-ceea-467a-9a3b-2c0b1e5f9d10", meal.analysis.id.value)
        assertEquals(listOf(PHOTO), photos.analyzed)
        assertEquals(listOf(PHOTO), photos.discarded)
    }

    @Test
    fun `confirming needs the plan answer`() {
        takeAndUsePhoto()

        viewModel.onConfirmProposal()

        assertTrue(viewModel.state.value.form.showPlanError)
        assertTrue(diary.photoMeals.isEmpty())
    }

    @Test
    fun `choosing an alternative logs it adjusted, keeping the proposal`() {
        takeAndUsePhoto()

        viewModel.onAlternativeSelected(0)
        viewModel.onPlanAnswer(false)
        viewModel.onConfirmProposal()

        val confirmation = diary.photoMeals.single().confirmation as PhotoConfirmation.Adjusted
        assertEquals(31L, confirmation.food.referenceFoodId)
        assertEquals(300.0, confirmation.portion.value, 0.0)
        assertEquals(PlanAdherence.OFF_PLAN, diary.photoMeals.single().planAdherence)
    }

    @Test
    fun `adjusting the grams in PT8 sends an adjusted confirmation`() {
        takeAndUsePhoto()
        viewModel.onAdjust()
        assertEquals(MealPhotoStep.ADJUST, viewModel.state.value.step)

        viewModel.onPortionChange("300")
        viewModel.onPlanAnswer(true)
        viewModel.onConfirmAdjustment()

        val confirmation = diary.photoMeals.single().confirmation as PhotoConfirmation.Adjusted
        assertEquals(12L, confirmation.food.referenceFoodId)
        assertEquals(300.0, confirmation.portion.value, 0.0)
    }

    @Test
    fun `PT8 rejects an invalid portion`() {
        takeAndUsePhoto()
        viewModel.onAdjust()

        viewModel.onPortionChange("0")
        viewModel.onPlanAnswer(true)
        viewModel.onConfirmAdjustment()

        assertTrue(viewModel.state.value.form.showPortionError)
        assertTrue(diary.photoMeals.isEmpty())
    }

    @Test
    fun `a dish that was not recognized shows PT7_3 and offers logging by hand`() = runTest {
        photos.analyzeResults = listOf(failureOf(DomainError.Validation("PhotoNotRecognized")))

        takeAndUsePhoto()

        assertEquals(MealPhotoStep.NOT_RECOGNIZED, viewModel.state.value.step)
        assertEquals(listOf(PHOTO), photos.discarded)
        viewModel.onRegisterManually()
        viewModel.events.test { assertEquals(MealPhotoEvent.OpenManual, awaitItem()) }
    }

    @Test
    fun `consent withdrawn on the server sends the patient to logging by hand`() = runTest {
        photos.analyzeResults = listOf(failureOf(DomainError.Forbidden("AiConsentRequired")))

        takeAndUsePhoto()

        viewModel.events.test { assertEquals(MealPhotoEvent.OpenManual, awaitItem()) }
        assertEquals(listOf(PHOTO), photos.discarded)
    }

    @Test
    fun `AI turned off on the server also goes to logging by hand`() = runTest {
        photos.analyzeResults = listOf(failureOf(DomainError.Unexpected("AiFeatureDisabled")))

        takeAndUsePhoto()

        viewModel.events.test { assertEquals(MealPhotoEvent.OpenManual, awaitItem()) }
    }

    @Test
    fun `the daily quota shows its own message`() {
        photos.analyzeResults = listOf(failureOf(DomainError.Unexpected("AiRateLimited")))

        takeAndUsePhoto()

        assertEquals(MealPhotoStep.ANALYSIS_FAILED, viewModel.state.value.step)
        assertEquals(AnalysisFailure.RATE_LIMITED, viewModel.state.value.failure)
    }

    @Test
    fun `offline the photo is kept on the phone to analyze it later`() = runTest {
        photos.analyzeResults = listOf(failureOf(DomainError.Network))

        takeAndUsePhoto()

        assertEquals(MealPhotoStep.SAVED_OFFLINE, viewModel.state.value.step)
        assertEquals(PHOTO, photos.pending.value.single().filePath)
        viewModel.onBack()
        viewModel.events.test { assertEquals(MealPhotoEvent.Exit, awaitItem()) }
        assertTrue(photos.discarded.isEmpty())
    }

    @Test
    fun `a pending photo is analyzed directly with its capture time`() {
        val captured = OffsetDateTime.parse("2026-10-07T07:30:00-05:00")
        photos.pending.value = listOf(PendingMealPhoto("pending-1", PHOTO, LocalTimestamp.restore(captured)))
        savedStateHandle[MealPhotoRoute.ARG_PENDING_PHOTO_ID] = "pending-1"

        assertEquals(MealPhotoStep.PROPOSAL, viewModel.state.value.step)
        assertEquals(captured, viewModel.state.value.form.mealTime)
        viewModel.onPlanAnswer(true)
        viewModel.onConfirmProposal()
        assertEquals(captured, diary.photoMeals.single().localTimestamp.value)
        assertTrue(photos.pending.value.isEmpty())
    }

    @Test
    fun `a failed save shows PT7_2 and the retry reuses the same client entry id`() {
        diary.logResult = failureOf(DomainError.Unexpected("HTTP_500"))
        takeAndUsePhoto()
        viewModel.onPlanAnswer(true)

        viewModel.onConfirmProposal()
        assertEquals(MealPhotoStep.SAVE_FAILED, viewModel.state.value.step)
        assertTrue(photos.discarded.isEmpty())

        diary.logResult = Result.success(MealLogOutcome.LOGGED)
        viewModel.onRetrySave()
        viewModel.onConfirmProposal()

        assertEquals(2, diary.photoMeals.size)
        assertEquals(diary.photoMeals[0].clientEntryId, diary.photoMeals[1].clientEntryId)
    }

    @Test
    fun `offline confirmation is queued and reported as such`() = runTest {
        diary.logResult = Result.success(MealLogOutcome.QUEUED)
        takeAndUsePhoto()
        viewModel.onPlanAnswer(true)

        viewModel.onConfirmProposal()

        viewModel.events.test { assertEquals(MealPhotoEvent.Logged(MealLogOutcome.QUEUED), awaitItem()) }
    }

    @Test
    fun `an expired window sends the patient to PT8 with the time error`() {
        diary.logResult = failureOf(DomainError.Validation("RetroactiveLoggingWindowExceeded"))
        takeAndUsePhoto()
        viewModel.onPlanAnswer(true)

        viewModel.onConfirmProposal()

        assertEquals(MealPhotoStep.ADJUST, viewModel.state.value.step)
        assertEquals(LocalTimestamp.Validity.TOO_OLD, viewModel.state.value.form.mealTimeError)
    }

    @Test
    fun `leaving the proposal without confirming keeps the meal «Por confirmar»`() = runTest {
        takeAndUsePhoto()

        viewModel.onBack()

        viewModel.events.test { assertEquals(MealPhotoEvent.Exit, awaitItem()) }
        assertEquals(1, diary.unconfirmed.size)
        assertTrue(diary.photoMeals.isEmpty())
        assertEquals(listOf(PHOTO), photos.discarded)
    }

    @Test
    fun `retaking from the preview deletes the photo and returns to the camera`() {
        viewModel.onCameraPermissionResult(true)
        viewModel.onPhotoCaptured(PHOTO)

        viewModel.onBack()

        assertEquals(MealPhotoStep.CAMERA, viewModel.state.value.step)
        assertNull(viewModel.state.value.photoPath)
        assertEquals(listOf(PHOTO), photos.discarded)
        assertFalse(viewModel.state.value.isSaving)
    }

    private companion object {
        const val PHOTO = "/cache/meal_photos/a.jpg"
    }
}
