package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.RecordSelfWeighInUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInEvent
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInUiState.WeightError
import pe.edu.upc.healthify.testing.FakeAccountRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSelfWeighInRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import java.time.LocalDateTime
import java.time.LocalTime

class SelfWeighInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSelfWeighInRepository()

    private val viewModel by lazy {
        SelfWeighInViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetAccountCreatedOnUseCase(FakeAccountRepository()),
            RecordSelfWeighInUseCase(repository),
            FakeConnectivityObserver(),
            fixedClock,
        )
    }

    @Test
    fun `the fasted question starts unanswered and saving shows every missing answer`() {
        assertNull(viewModel.state.value.fasted)

        viewModel.onSave()

        val state = viewModel.state.value
        assertEquals(WeightError.EMPTY, state.weightError)
        assertTrue(state.showFastedError)
        assertTrue(repository.recorded.isEmpty())
    }

    @Test
    fun `PT12_E a weight out of range is caught on the phone`() {
        viewModel.onWeightChange("684")
        viewModel.onFastedAnswer(true)

        viewModel.onSave()

        assertEquals(WeightError.OUT_OF_RANGE, viewModel.state.value.weightError)
        assertTrue(repository.recorded.isEmpty())
    }

    @Test
    fun `a moment older than 48 h is not accepted`() {
        viewModel.onWeightChange("68,4")
        viewModel.onFastedAnswer(false)
        viewModel.onTimeSelected(LocalDateTime.of(TODAY.minusDays(2), LocalTime.of(6, 0)))

        viewModel.onSave()

        assertEquals(LocalTimestamp.Validity.TOO_OLD, viewModel.state.value.timeError)
        assertTrue(repository.recorded.isEmpty())
    }

    @Test
    fun `a valid weigh-in is saved with the fasted answer and goes back to the trend`() = runTest {
        viewModel.events.test {
            viewModel.onWeightChange("68,4")
            viewModel.onFastedAnswer(false)
            viewModel.onSave()

            assertEquals(SelfWeighInEvent.Saved(SelfWeighInOutcome.RECORDED), awaitItem())
        }
        val sent = repository.recorded.single()
        assertEquals(68.4, sent.weight.value, 0.0)
        assertFalse(sent.fasted)
    }

    @Test
    fun `offline the weigh-in is queued and the screen closes the same way`() = runTest {
        repository.recordResult = Result.success(SelfWeighInOutcome.QUEUED)
        viewModel.events.test {
            viewModel.onWeightChange("70")
            viewModel.onFastedAnswer(true)
            viewModel.onSave()

            assertEquals(SelfWeighInEvent.Saved(SelfWeighInOutcome.QUEUED), awaitItem())
        }
    }

    @Test
    fun `the backend's implausible weight shows the range error, not the error screen`() {
        repository.recordResult = failureOf(DomainError.Validation("ImplausibleWeightValue"))
        viewModel.onWeightChange("68,4")
        viewModel.onFastedAnswer(true)

        viewModel.onSave()

        assertEquals(WeightError.OUT_OF_RANGE, viewModel.state.value.weightError)
        assertFalse(viewModel.state.value.saveFailed)
    }

    @Test
    fun `PT12_2 a failed save keeps what was typed and the retry resends the same weigh-in`() {
        repository.recordResult = failureOf(DomainError.Unexpected("HTTP_500"))
        viewModel.onWeightChange("68,4")
        viewModel.onFastedAnswer(true)
        viewModel.onSave()
        assertTrue(viewModel.state.value.saveFailed)
        assertEquals("68,4", viewModel.state.value.weightText)

        repository.recordResult = Result.success(SelfWeighInOutcome.RECORDED)
        viewModel.onRetry()

        assertFalse(viewModel.state.value.saveFailed)
        assertEquals(2, repository.recorded.size)
        assertEquals(repository.recorded[0].clientEntryId, repository.recorded[1].clientEntryId)
    }
}
