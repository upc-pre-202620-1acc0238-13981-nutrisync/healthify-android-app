package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.RefreshLocalFoodCatalogUseCase
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.SearchFoodsUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogManualMealUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.presentation.navigation.ManualMealRoute
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealEvent
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealUiState
import pe.edu.upc.healthify.testing.FakeAccountRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeDiaryRepository
import pe.edu.upc.healthify.testing.FakeReferenceFoodRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.referenceFood
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ManualMealViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val foods = FakeReferenceFoodRepository()
    private val diary = FakeDiaryRepository()
    private val savedStateHandle = SavedStateHandle()

    private val viewModel by lazy {
        ManualMealViewModel(
            savedStateHandle,
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetAccountCreatedOnUseCase(FakeAccountRepository()),
            SearchFoodsUseCase(foods),
            RefreshLocalFoodCatalogUseCase(foods),
            LogManualMealUseCase(diary),
            FakeConnectivityObserver(),
            fixedClock,
        )
    }

    private val quinua = referenceFood(1, "Quinua cocida")

    @Test
    fun `search shows the phone catalog first and then what the server adds`() = runTest(mainDispatcherRule.dispatcher) {
        foods.local = listOf(quinua)
        foods.remote = Result.success(listOf(referenceFood(2, "Quinua con leche"), quinua))

        viewModel.onQueryChange("quinua")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(1L, 2L), state.results.map { it.id.value })
        assertEquals("quinua", state.searchedTerm)
        assertFalse(state.localOnly)
        assertEquals(1, foods.refreshes)
    }

    @Test
    fun `offline the search answers from the saved catalog only (PT10 note)`() = runTest(mainDispatcherRule.dispatcher) {
        foods.local = listOf(quinua)
        foods.remote = failureOf(DomainError.Network)

        viewModel.onQueryChange("qui")
        advanceUntilIdle()

        assertEquals(listOf(quinua), viewModel.state.value.results)
        assertTrue(viewModel.state.value.localOnly)
    }

    @Test
    fun `nothing found is PT10, never an error`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onQueryChange("chaufa de cuy")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.notFound)
        viewModel.onSearchAgain()
        assertFalse(viewModel.state.value.notFound)
        assertNull(viewModel.state.value.searchedTerm)
    }

    @Test
    fun `logging needs a food, a valid portion and the plan answer`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ManualMealUiState.FoodError.NOT_CHOSEN, state.foodError)
        assertTrue(state.form.showPortionError)
        assertTrue(state.form.showPlanError)
        assertTrue(diary.manualMeals.isEmpty())
    }

    @Test
    fun `a meal older than 48 h is not sent`() = runTest(mainDispatcherRule.dispatcher) {
        selectQuinua()
        viewModel.onPortionChange("150")
        viewModel.onPlanAnswer(true)
        // Reloj fijo: 2026-10-07T13:00Z → hace 3 días.
        viewModel.onMealTimeSelected(LocalDateTime.parse("2026-10-04T12:00:00"))

        assertEquals(LocalTimestamp.Validity.TOO_OLD, viewModel.state.value.form.mealTimeError)
        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(diary.manualMeals.isEmpty())
    }

    @Test
    fun `a valid meal is logged with its plan answer and the diary is opened`() = runTest(mainDispatcherRule.dispatcher) {
        selectQuinua()
        viewModel.onPortionChange("150,5")
        viewModel.onPlanAnswer(false)

        viewModel.onSubmit()
        advanceUntilIdle()

        val meal = diary.manualMeals.single()
        assertEquals(1L, meal.food.referenceFoodId)
        assertEquals(150.5, meal.portion.value, 0.0)
        assertEquals(PlanAdherence.OFF_PLAN, meal.planAdherence)
        viewModel.events.test { assertEquals(ManualMealEvent.Logged(MealLogOutcome.LOGGED), awaitItem()) }
    }

    @Test
    fun `a failed log shows PT10_3 and the retry resends the same client entry id`() = runTest(mainDispatcherRule.dispatcher) {
        diary.logResult = failureOf(DomainError.Unexpected("HTTP_503"))
        selectQuinua()
        viewModel.onPortionChange("150")
        viewModel.onPlanAnswer(true)

        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saveFailed)

        diary.logResult = Result.success(MealLogOutcome.QUEUED)
        viewModel.onRetry()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.saveFailed)
        assertEquals(diary.manualMeals[0].clientEntryId, diary.manualMeals[1].clientEntryId)
        viewModel.events.test { assertEquals(ManualMealEvent.Logged(MealLogOutcome.QUEUED), awaitItem()) }
    }

    @Test
    fun `the backend window error lands on the meal time field`() = runTest(mainDispatcherRule.dispatcher) {
        diary.logResult = failureOf(DomainError.Validation("RetroactiveLoggingWindowExceeded"))
        selectQuinua()
        viewModel.onPortionChange("150")
        viewModel.onPlanAnswer(true)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(LocalTimestamp.Validity.TOO_OLD, viewModel.state.value.form.mealTimeError)
        assertFalse(viewModel.state.value.saveFailed)
    }

    @Test
    fun `in pick mode the chosen food goes back to PT8`() = runTest(mainDispatcherRule.dispatcher) {
        savedStateHandle[ManualMealRoute.ARG_PICK_FOOD_ONLY] = true
        selectQuinua()

        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.events.test { assertEquals(ManualMealEvent.FoodPicked(1, "Quinua cocida"), awaitItem()) }
        assertTrue(diary.manualMeals.isEmpty())
    }

    private suspend fun kotlinx.coroutines.test.TestScope.selectQuinua() {
        foods.local = listOf(quinua)
        viewModel.onQueryChange("quinua")
        advanceUntilIdle()
        viewModel.onFoodSelected(quinua)
    }
}
