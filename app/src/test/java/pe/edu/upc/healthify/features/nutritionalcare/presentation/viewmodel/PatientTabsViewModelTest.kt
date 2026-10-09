package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetLinkTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientSummaryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanHistoryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPractitionerRecordUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SavePatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.StartOrResumeConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationInProgress
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NutritionPlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SummaryFollowUp
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.BaselineEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientTabEvent
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeConsultationRepository
import pe.edu.upc.healthify.testing.FakeNutritionPlanRepository
import pe.edu.upc.healthify.testing.FakePatientBaselineRepository
import pe.edu.upc.healthify.testing.FakePatientSummaryRepository
import pe.edu.upc.healthify.testing.FakePractitionerRecordRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.consultation
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.patientArgs
import pe.edu.upc.healthify.testing.patientSummary
import pe.edu.upc.healthify.testing.readStatus
import java.time.Instant
import java.time.LocalDate

class PatientTabsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val summaries = FakePatientSummaryRepository()
    private val consultations = FakeConsultationRepository()
    private val baselines = FakePatientBaselineRepository()
    private val connectivity = FakeConnectivityObserver()
    private val clock = fixedClock()

    private fun summaryViewModel() = PatientSummaryViewModel(
        savedStateHandle = patientArgs(),
        getPatientSummary = GetPatientSummaryUseCase(summaries),
        startOrResumeConsultation = StartOrResumeConsultationUseCase(consultations),
        connectivityObserver = connectivity,
        clock = clock,
    )

    @Test
    fun `PAC-0 without baseline opens EV-1 to record it`() = runTest {
        summaries.result = Result.success(patientSummary(baseline = null, activePlanVersion = null))
        val viewModel = summaryViewModel()
        assertTrue(viewModel.state.value.summary!!.isNew)

        viewModel.events.test {
            viewModel.onPrimaryAction()
            assertEquals(PatientTabEvent.OpenBaseline(editing = false), awaitItem())
        }
        assertTrue(consultations.starts.isEmpty())
    }

    @Test
    fun `PAC-1-C continue consultation opens the step in progress without starting another`() = runTest {
        summaries.result = Result.success(
            patientSummary(inProgress = ConsultationInProgress(ConsultationId(9), ConsultationStep.TARGETS, CONSULTATIONS_NOW)),
        )
        val viewModel = summaryViewModel()

        viewModel.events.test {
            viewModel.onPrimaryAction()
            assertEquals(PatientTabEvent.OpenConsultationStep(ConsultationStep.TARGETS), awaitItem())
        }
        assertTrue(consultations.starts.isEmpty())
    }

    @Test
    fun `PAC-1 start consultation opens step 1 and links the follow-up only if it is today`() = runTest {
        summaries.result = Result.success(
            patientSummary(nextFollowUp = SummaryFollowUp(31, CONSULTATIONS_NOW.plusSeconds(3_600))),
        )
        val viewModel = summaryViewModel()

        viewModel.events.test {
            viewModel.onPrimaryAction()
            assertEquals(PatientTabEvent.OpenConsultationStep(ConsultationStep.MEASUREMENT), awaitItem())
        }
        assertEquals(listOf<Long?>(31), consultations.starts)

        summaries.result = Result.success(
            patientSummary(nextFollowUp = SummaryFollowUp(32, Instant.parse("2026-09-18T15:00:00Z"))),
        )
        viewModel.onRetry()
        viewModel.onPrimaryAction()
        assertEquals(null, consultations.starts.last())
    }

    @Test
    fun `starting when one is already in progress resumes it`() = runTest {
        consultations.startResult = failureOf(DomainError.Conflict("ConsultationAlreadyInProgress"))
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.DIAGNOSIS))
        val viewModel = summaryViewModel()

        viewModel.events.test {
            viewModel.onPrimaryAction()
            assertEquals(PatientTabEvent.OpenConsultationStep(ConsultationStep.DIAGNOSIS), awaitItem())
        }
    }

    @Test
    fun `starting without baseline on the server opens EV-1 and other errors show the retry dialog`() = runTest {
        consultations.startResult = failureOf(DomainError.Validation("BaselineRequired"))
        val viewModel = summaryViewModel()
        viewModel.events.test {
            viewModel.onPrimaryAction()
            assertEquals(PatientTabEvent.OpenBaseline(editing = false), awaitItem())
        }

        consultations.startResult = failureOf(DomainError.Network)
        viewModel.onPrimaryAction()
        assertTrue(viewModel.state.value.showStartError)
        viewModel.onStartErrorDismissed()
        assertFalse(viewModel.state.value.showStartError)
    }

    @Test
    fun `summary offline without a previous read asks for a connection, and reloads when it comes back`() {
        connectivity.online.value = false
        summaries.result = failureOf(DomainError.Network)
        val viewModel = summaryViewModel()
        assertTrue(viewModel.state.value.loadFailed)
        assertTrue(viewModel.state.value.isOffline)

        summaries.result = Result.success(patientSummary())
        connectivity.online.value = true
        assertFalse(viewModel.state.value.loadFailed)
        assertEquals("Ana Flores", viewModel.state.value.summary?.fullName)
    }

    // ----- PAC-4 · Plan -----

    private val careLinks = FakeCareLinkRepository()
    private val plans = FakeNutritionPlanRepository()

    private fun planViewModel() = PatientPlanViewModel(
        savedStateHandle = patientArgs(),
        getPlanHistory = GetPlanHistoryUseCase(plans),
        getLinkTargetsReadStatus = GetLinkTargetsReadStatusUseCase(careLinks),
        startOrResumeConsultation = StartOrResumeConsultationUseCase(consultations),
        connectivityObserver = connectivity,
    )

    private fun version(number: Int, active: Boolean) = NutritionPlanVersion(
        number,
        active,
        Instant.parse("2026-09-04T15:00:00Z"),
        Targets(1796.0, 119.0, 195.0, 60.0),
        emptyList(),
        emptyList(),
        null,
    )

    @Test
    fun `PAC-4 reads the history and whether the patient saw the targets`() {
        plans.result = Result.success(PlanHistory.of(listOf(version(2, false), version(3, true))))
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = null))

        val state = planViewModel().state.value

        assertEquals(3, state.history?.active?.version)
        assertEquals(careLinks.readStatusResult.getOrNull(), state.readStatus)
    }

    @Test
    fun `PAC-4 without the read status still shows the plan`() {
        plans.result = Result.success(PlanHistory.of(listOf(version(1, true))))
        careLinks.readStatusResult = failureOf(DomainError.Forbidden())

        val state = planViewModel().state.value

        assertEquals(1, state.history?.active?.version)
        assertNull(state.readStatus)
    }

    @Test
    fun `PAC-4 adjust plan opens the guided consultation`() = runTest {
        plans.result = Result.success(PlanHistory.of(listOf(version(1, true))))
        val viewModel = planViewModel()

        viewModel.events.test {
            viewModel.onAdjustPlan()
            assertEquals(PatientTabEvent.OpenConsultationStep(ConsultationStep.MEASUREMENT), awaitItem())
        }
    }

    // ----- PAC-3 · Expediente -----

    @Test
    fun `PAC-3 failure without a previous read shows the error`() {
        val records = FakePractitionerRecordRepository().apply { result = failureOf(DomainError.Forbidden("AccessNotAllowed")) }
        val viewModel = PractitionerRecordViewModel(patientArgs(), GetPractitionerRecordUseCase(records), connectivity)

        assertTrue(viewModel.state.value.loadFailed)
        assertFalse(viewModel.state.value.isOffline)
    }

    // ----- EV-1 · Datos base -----

    private fun baselineViewModel(editing: Boolean = false) = BaselineViewModel(
        savedStateHandle = patientArgs(editing = editing),
        getPatientBaseline = GetPatientBaselineUseCase(baselines),
        savePatientBaseline = SavePatientBaselineUseCase(baselines),
        startOrResumeConsultation = StartOrResumeConsultationUseCase(consultations),
        connectivityObserver = connectivity,
        clock = clock,
    )

    @Test
    fun `EV-1 marks the missing fields and computes the age by itself`() {
        val viewModel = baselineViewModel()

        viewModel.onSaveAndStart()
        assertEquals(
            setOf(BaselineProblem.BIRTH_DATE_REQUIRED, BaselineProblem.SEX_REQUIRED, BaselineProblem.HEIGHT_REQUIRED),
            viewModel.state.value.problems,
        )

        viewModel.onBirthDateChange(LocalDate.of(1995, 2, 15))
        assertEquals(31, viewModel.state.value.ageYears)
        assertTrue(baselines.recorded.isEmpty())
    }

    @Test
    fun `EV-1 save and start records the baseline and opens step 1`() = runTest {
        val viewModel = baselineViewModel()
        viewModel.onBirthDateChange(LocalDate.of(1995, 2, 15))
        viewModel.onSexChange(BiologicalSex.FEMALE)
        viewModel.onHeightChange("168")
        viewModel.onConditionToggle(MedicalCondition.HYPOTHYROIDISM)

        viewModel.events.test {
            viewModel.onSaveAndStart()
            assertEquals(BaselineEvent.ConsultationStarted(ConsultationStep.MEASUREMENT), awaitItem())
        }
        assertEquals(setOf(MedicalCondition.HYPOTHYROIDISM), baselines.recorded.single().conditions)
        assertTrue(baselines.updated.isEmpty())
    }

    @Test
    fun `EV-1 recorded elsewhere is saved as an edit`() = runTest {
        baselines.recordResult = failureOf(DomainError.Conflict("BaselineAlreadyRecorded"))
        val viewModel = baselineViewModel()
        viewModel.onBirthDateChange(LocalDate.of(1995, 2, 15))
        viewModel.onSexChange(BiologicalSex.FEMALE)
        viewModel.onHeightChange("168")

        viewModel.events.test {
            viewModel.onSaveAndExit()
            assertEquals(BaselineEvent.Saved, awaitItem())
        }
        assertEquals(1, baselines.updated.size)
    }

    @Test
    fun `EV-1 editing prefills and saves with PUT`() = runTest {
        val viewModel = baselineViewModel(editing = true)
        val state = viewModel.state.value
        assertEquals("168", state.heightText)
        assertEquals(BiologicalSex.FEMALE, state.sex)

        viewModel.onHeightChange("169.5")
        viewModel.events.test {
            viewModel.onSaveAndExit()
            assertEquals(BaselineEvent.Saved, awaitItem())
        }
        assertEquals(169.5, baselines.updated.single().height.value, 0.0)
        assertTrue(baselines.recorded.isEmpty())
    }

    @Test
    fun `EV-1 invalid height from the backend marks the field`() {
        baselines.recordResult = failureOf(DomainError.Validation("InvalidHeight"))
        val viewModel = baselineViewModel()
        viewModel.onBirthDateChange(LocalDate.of(1995, 2, 15))
        viewModel.onSexChange(BiologicalSex.MALE)
        viewModel.onHeightChange("170")

        viewModel.onSaveAndExit()

        assertEquals(setOf(BaselineProblem.HEIGHT_OUT_OF_RANGE), viewModel.state.value.problems)
        assertNull(viewModel.state.value.saveFailure)
    }
}
