package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetConsultationInProgressUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.IssueConsultationDiagnosisUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.PrescribeConsultationTargetsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.ProposeConsultationTargetsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.PublishConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.RecordConsultationMeasurementUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SavePublicationDraftUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SuggestDiagnosisUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SuggestGuidelinesUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.FieldProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.GuidelineSuggestions
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PublicationDraft
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.OverrideForm
import pe.edu.upc.healthify.testing.CONSULTATIONS_NOW
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeConsultationRepository
import pe.edu.upc.healthify.testing.FakePatientBaselineRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.consultation
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.issuedDiagnosis
import pe.edu.upc.healthify.testing.patientArgs
import pe.edu.upc.healthify.testing.prescribedTargets
import pe.edu.upc.healthify.testing.savedMeasurement

class ConsultationStepsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val consultations = FakeConsultationRepository()
    private val baselines = FakePatientBaselineRepository()
    private val connectivity = FakeConnectivityObserver()
    private val getInProgress = GetConsultationInProgressUseCase(consultations)

    // ----- EV-2 · Medición -----

    private fun measurementViewModel() = MeasurementStepViewModel(
        savedStateHandle = patientArgs(),
        getConsultationInProgress = getInProgress,
        getPatientBaseline = GetPatientBaselineUseCase(baselines),
        recordMeasurement = RecordConsultationMeasurementUseCase(consultations),
        connectivityObserver = connectivity,
    )

    @Test
    fun `EV-2 resumes from the consultation in progress with the saved measurement and the check-in`() {
        val checkIn = PatientCheckIn("Fair", listOf("Dinners"), listOf("¿Puedo comer fuera?"), CONSULTATIONS_NOW)
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.DIAGNOSIS, measurement = savedMeasurement(), checkIn = checkIn),
        )

        val state = measurementViewModel().state.value

        assertFalse(state.load.isLoading)
        assertEquals("74.2", state.form.weight)
        assertEquals("88", state.form.waist)
        assertEquals(setOf(ProtocolCheck.FASTING, ProtocolCheck.NO_SHOES), state.form.protocolChecks)
        assertEquals(ActivityLevel.MODERATE, state.form.activityLevel)
        assertEquals(26.3, state.bmi!!, 0.0)
        assertEquals(checkIn, state.checkIn)
        assertEquals(168.0, state.baseline?.heightCm)
    }

    @Test
    fun `EV-2 calculates the BMI live with the baseline height`() {
        val viewModel = measurementViewModel()

        viewModel.onWeightChange("80")
        assertEquals(28.3, viewModel.state.value.bmi!!, 0.0)

        viewModel.onWeightChange("8")
        assertNull(viewModel.state.value.bmi)
    }

    @Test
    fun `EV-2 does not send anything until the required fields are valid`() {
        val viewModel = measurementViewModel()
        viewModel.onWeightChange("400")

        viewModel.onContinue()

        assertTrue(consultations.measurements.isEmpty())
        assertEquals(
            mapOf(
                MeasurementField.WEIGHT to FieldProblem.OUT_OF_RANGE,
                MeasurementField.PROTOCOL to FieldProblem.REQUIRED,
                MeasurementField.ACTIVITY to FieldProblem.REQUIRED,
            ),
            viewModel.state.value.problems,
        )
    }

    @Test
    fun `EV-2 saves the measurement and continues`() = runTest {
        val viewModel = measurementViewModel()
        viewModel.onWeightChange("74,2")
        viewModel.onProtocolToggle(ProtocolCheck.FASTING)
        viewModel.onActivityChange(ActivityLevel.LIGHT)

        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertEquals(74.2, consultations.measurements.single().weightKg, 0.0)
    }

    @Test
    fun `EV-2 does not repeat step 1 when nothing changed, so the diagnosis is kept`() = runTest {
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.TARGETS, measurement = savedMeasurement()))
        val viewModel = measurementViewModel()

        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertTrue(consultations.measurements.isEmpty())
    }

    @Test
    fun `EV-2 back asks EV-2-S and leaving keeps what was saved`() = runTest {
        val viewModel = measurementViewModel()

        viewModel.onBack()
        assertTrue(viewModel.state.value.showExitDialog)
        viewModel.onStay()
        assertFalse(viewModel.state.value.showExitDialog)

        viewModel.events.test {
            viewModel.onBack()
            viewModel.onExit()
            assertEquals(ConsultationStepEvent.Exit, awaitItem())
        }
        assertTrue(consultations.measurements.isEmpty())
    }

    @Test
    fun `EV-2 with no consultation in progress goes back to the patient`() = runTest {
        consultations.inProgressResult = Result.success(null)
        val viewModel = measurementViewModel()

        viewModel.events.test { assertEquals(ConsultationStepEvent.ConsultationClosed, awaitItem()) }
    }

    @Test
    fun `EV-2 offline load shows the connection state and retries`() {
        consultations.inProgressResult = failureOf(DomainError.Network)
        val viewModel = measurementViewModel()
        assertTrue(viewModel.state.value.load.loadFailed)
        assertTrue(viewModel.state.value.load.isOffline)

        consultations.inProgressResult = Result.success(consultation())
        viewModel.onRetry()
        assertFalse(viewModel.state.value.load.loadFailed)
    }

    // ----- EV-3 · Diagnóstico -----

    private fun diagnosisViewModel() = DiagnosisStepViewModel(
        savedStateHandle = patientArgs(),
        getConsultationInProgress = getInProgress,
        suggestDiagnosis = SuggestDiagnosisUseCase(consultations),
        issueDiagnosis = IssueConsultationDiagnosisUseCase(consultations),
        connectivityObserver = connectivity,
    )

    @Test
    fun `EV-3 resumes with the saved diagnosis and shows the AI suggestion`() {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis(DiagnosisCode.OBESITY_GRADE_I)),
        )

        val state = diagnosisViewModel().state.value

        assertEquals(DiagnosisCode.OBESITY_GRADE_I, state.selected)
        assertEquals(DiagnosisCode.OVERWEIGHT_GRADE_I, state.suggestion?.code)
        assertTrue(state.suggestion!!.isFromAi)
    }

    @Test
    fun `EV-3 use suggestion records the AI generation`() = runTest {
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.DIAGNOSIS, savedMeasurement()))
        val viewModel = diagnosisViewModel()
        viewModel.onUseSuggestion()

        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertEquals(
            DiagnosisChoice.AcceptAiSuggestion(DiagnosisCode.OVERWEIGHT_GRADE_I, 77, "IMC 26.3 kg/m²"),
            consultations.diagnoses.single(),
        )
    }

    @Test
    fun `EV-3 choose other needs a pick from the closed list and is a choice of the practitioner`() = runTest {
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.DIAGNOSIS, savedMeasurement()))
        val viewModel = diagnosisViewModel()
        viewModel.onUseSuggestion()
        viewModel.onChooseOther()

        viewModel.onContinue()
        assertTrue(viewModel.state.value.showRequired)
        assertTrue(consultations.diagnoses.isEmpty())

        viewModel.onSelect(DiagnosisCode.NORMAL_WEIGHT)
        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertEquals(DiagnosisChoice.Selected(DiagnosisCode.NORMAL_WEIGHT), consultations.diagnoses.single())
    }

    @Test
    fun `EV-3 without a suggestion still offers the list`() {
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.DIAGNOSIS, savedMeasurement()))
        consultations.suggestionResult = failureOf(DomainError.Network)

        val state = diagnosisViewModel().state.value

        assertNull(state.suggestion)
        assertFalse(state.isSuggesting)
        assertFalse(state.load.loadFailed)
    }

    @Test
    fun `EV-3 coming back with the pending diagnosis keeps it and continues without issuing it again`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(
                ConsultationStep.TARGETS,
                savedMeasurement(),
                issuedDiagnosis(DiagnosisCode.OVERWEIGHT_GRADE_I, DiagnosisSource.AI_SUGGESTION_ACCEPTED, pending = true),
            ),
        )
        val viewModel = diagnosisViewModel()

        assertEquals(DiagnosisCode.OVERWEIGHT_GRADE_I, viewModel.state.value.selected)
        assertTrue(viewModel.state.value.usesSuggestion)
        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertTrue(consultations.diagnoses.isEmpty())
    }

    @Test
    fun `EV-3 changing the pending diagnosis issues the new one`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis(pending = true)),
        )
        val viewModel = diagnosisViewModel()
        viewModel.onSelect(DiagnosisCode.OBESITY_GRADE_I)

        viewModel.events.test {
            viewModel.onContinue()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertEquals(DiagnosisChoice.Selected(DiagnosisCode.OBESITY_GRADE_I), consultations.diagnoses.single())
    }

    @Test
    fun `EV-3 without step 1 goes back to the measurement`() = runTest {
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.MEASUREMENT))
        val viewModel = diagnosisViewModel()

        viewModel.events.test { assertEquals(ConsultationStepEvent.Back, awaitItem()) }
    }

    // ----- EV-4 · Metas -----

    private fun targetsViewModel() = TargetsStepViewModel(
        savedStateHandle = patientArgs(),
        getConsultationInProgress = getInProgress,
        proposeTargets = ProposeConsultationTargetsUseCase(consultations),
        prescribeTargets = PrescribeConsultationTargetsUseCase(consultations),
        connectivityObserver = connectivity,
    )

    @Test
    fun `EV-4 calculates with the default parameters and accepts them as proposed`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis()),
        )
        val viewModel = targetsViewModel()
        assertEquals(listOf(null), consultations.proposals)
        assertNotNull(viewModel.state.value.proposal)

        viewModel.events.test {
            viewModel.onAccept()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertEquals(TargetPrescription.AcceptedAsProposed, consultations.prescriptions.single())
    }

    @Test
    fun `EV-4 with the pending diagnosis of step 2 calculates and does not go back`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis(pending = true)),
        )
        val viewModel = targetsViewModel()

        viewModel.events.test { expectNoEvents() }
        assertEquals(listOf(null), consultations.proposals)
        assertNotNull(viewModel.state.value.proposal)
    }

    @Test
    fun `EV-4 without a diagnosis goes back to step 2`() = runTest {
        // NC-7: si la medición se repitió, el backend descarta el diagnóstico y la consulta llega sin él.
        consultations.inProgressResult = Result.success(consultation(ConsultationStep.DIAGNOSIS, savedMeasurement()))
        val viewModel = targetsViewModel()

        viewModel.events.test { assertEquals(ConsultationStepEvent.Back, awaitItem()) }
        assertTrue(consultations.proposals.isEmpty())
    }

    @Test
    fun `EV-4 own values require a reason before anything is sent`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis()),
        )
        val viewModel = targetsViewModel()
        viewModel.onWriteOwnValues()
        assertEquals("1796", viewModel.state.value.override.energy)

        viewModel.onOverrideChange(viewModel.state.value.override.copy(energy = "1700"))
        viewModel.onSaveOverride()

        assertEquals(setOf(OverrideField.REASON), viewModel.state.value.overrideProblems)
        assertTrue(consultations.prescriptions.isEmpty())

        viewModel.onOverrideChange(viewModel.state.value.override.copy(reason = "Ajuste por su trabajo"))
        assertTrue(viewModel.state.value.overrideProblems.isEmpty())
        viewModel.events.test {
            viewModel.onSaveOverride()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        val overridden = consultations.prescriptions.single() as TargetPrescription.Overridden
        assertEquals(1700.0, overridden.targets.energyKcal, 0.0)
        assertEquals("Ajuste por su trabajo", overridden.reason.value)
    }

    @Test
    fun `EV-4 shows the reason field again when the backend asks for it`() {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis()),
        )
        consultations.prescribeResult = failureOf(DomainError.Validation("OverrideReasonRequired"))
        val viewModel = targetsViewModel()
        viewModel.onWriteOwnValues()
        viewModel.onOverrideChange(OverrideForm("1700", "120", "180", "55", "Algo"))

        viewModel.onSaveOverride()

        assertTrue(viewModel.state.value.showOverride)
        assertEquals(setOf(OverrideField.REASON), viewModel.state.value.overrideProblems)
    }

    @Test
    fun `EV-4 resuming with targets already prescribed does not recalculate`() = runTest {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.PUBLICATION, savedMeasurement(), issuedDiagnosis(), prescribedTargets()),
        )
        val viewModel = targetsViewModel()

        assertTrue(consultations.proposals.isEmpty())
        assertEquals(1796.0, viewModel.state.value.proposal!!.proposal.energyKcal, 0.0)
        viewModel.events.test {
            viewModel.onAccept()
            assertEquals(ConsultationStepEvent.Continue, awaitItem())
        }
        assertTrue(consultations.prescriptions.isEmpty())
    }

    @Test
    fun `EV-4 change parameters recalculates with the new ones`() {
        consultations.inProgressResult = Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis()),
        )
        val viewModel = targetsViewModel()
        viewModel.onChangeParameters()
        val form = viewModel.state.value.parameters!!
        assertEquals("500", form.deficit)

        viewModel.onParametersChange(form.copy(deficit = "2000"))
        viewModel.onApplyParameters()
        assertTrue(viewModel.state.value.parametersInvalid)

        viewModel.onParametersChange(form.copy(deficit = "300"))
        viewModel.onApplyParameters()
        assertEquals(300.0, consultations.proposals.last()!!.deficitValue, 0.0)
        assertFalse(viewModel.state.value.showParameters)
    }

    // ----- EV-5 · Publicación -----

    private fun publicationViewModel() = PublicationStepViewModel(
        savedStateHandle = patientArgs(),
        getConsultationInProgress = getInProgress,
        suggestGuidelines = SuggestGuidelinesUseCase(consultations),
        saveDraft = SavePublicationDraftUseCase(consultations),
        publishConsultation = PublishConsultationUseCase(consultations),
        connectivityObserver = connectivity,
    )

    private fun readyToPublish(draft: PublicationDraft? = null) = Result.success(
        consultation(ConsultationStep.PUBLICATION, savedMeasurement(), issuedDiagnosis(), prescribedTargets(), draft),
    )

    @Test
    fun `EV-5 resumes from the saved draft`() {
        consultations.inProgressResult = readyToPublish(
            PublicationDraft(
                restrictions = setOf(PlanRestrictionCode.SHELLFISH_FREE),
                guidelines = setOf(PlanGuidelineCode.REDUCE_SALT),
                customGuidelines = listOf("Caminar 20 minutos", "x"),
                patientMessage = "Vamos bien",
            ),
        )
        consultations.guidelinesResult = Result.success(GuidelineSuggestions(listOf(PlanGuidelineCode.DRINK_2L_WATER), true))

        val state = publicationViewModel().state.value

        assertEquals(setOf(PlanRestrictionCode.SHELLFISH_FREE), state.restrictions)
        assertEquals(setOf(PlanGuidelineCode.REDUCE_SALT), state.guidelines)
        assertEquals(listOf("Caminar 20 minutos"), state.customGuidelines)
        assertEquals("Vamos bien", state.patientMessage)
        assertTrue(state.suggestedByAi)
    }

    @Test
    fun `EV-5 without a draft starts with the suggested guidelines`() {
        consultations.inProgressResult = readyToPublish()
        consultations.guidelinesResult = Result.success(
            GuidelineSuggestions(listOf(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.REDUCE_SALT), false),
        )

        val state = publicationViewModel().state.value

        assertEquals(setOf(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.REDUCE_SALT), state.guidelines)
        assertFalse(state.suggestedByAi)
    }

    @Test
    fun `EV-5 retrying after an error publishes with the same idempotency key`() = runTest {
        consultations.inProgressResult = readyToPublish()
        consultations.publishResults = listOf(
            failureOf(DomainError.Network),
            failureOf(DomainError.Unexpected("HTTP_503")),
            Result.success(consultation(isInProgress = false)),
        )
        val viewModel = publicationViewModel()
        viewModel.onRestrictionToggle(PlanRestrictionCode.VEGAN)

        viewModel.onPublish()
        assertTrue(viewModel.state.value.publishFailed)
        viewModel.onRetryPublish()
        assertTrue(viewModel.state.value.publishFailed)
        viewModel.events.test {
            viewModel.onRetryPublish()
            assertEquals(ConsultationStepEvent.Published, awaitItem())
        }

        val keys = consultations.publications.map { it.second }.distinct()
        assertEquals(1, keys.size)
        assertEquals(3, consultations.publications.size)
        assertEquals(setOf(PlanRestrictionCode.VEGAN), consultations.publications.last().first.restrictions)
        // Lo escrito se guardó como borrador antes de publicar: no se pierde.
        assertEquals(setOf(PlanRestrictionCode.VEGAN), consultations.drafts.first().restrictions)
    }

    @Test
    fun `EV-5 keeps the key across a recreated screen`() {
        consultations.inProgressResult = readyToPublish()
        consultations.publishResults = listOf(failureOf(DomainError.Network))
        val handle = patientArgs()
        fun viewModel() = PublicationStepViewModel(
            handle,
            getInProgress,
            SuggestGuidelinesUseCase(consultations),
            SavePublicationDraftUseCase(consultations),
            PublishConsultationUseCase(consultations),
            connectivity,
        )

        viewModel().onPublish()
        viewModel().onPublish()

        assertEquals(1, consultations.publications.map { it.second }.distinct().size)
    }

    @Test
    fun `EV-5 custom guidelines need 3 to 140 characters and at most 5`() {
        consultations.inProgressResult = readyToPublish()
        val viewModel = publicationViewModel()
        viewModel.onAddCustomClick()
        viewModel.onCustomDraftChange("ab")
        viewModel.onConfirmCustom()
        assertTrue(viewModel.state.value.customError)

        repeat(5) {
            viewModel.onAddCustomClick()
            viewModel.onCustomDraftChange("Indicación $it")
            viewModel.onConfirmCustom()
        }
        assertEquals(5, viewModel.state.value.customGuidelines.size)
        viewModel.onAddCustomClick()
        viewModel.onCustomDraftChange("Una más")
        viewModel.onConfirmCustom()
        assertTrue(viewModel.state.value.customError)
        assertEquals(5, viewModel.state.value.customGuidelines.size)
    }

    @Test
    fun `EV-5 back saves the draft once and goes to step 3`() = runTest {
        consultations.inProgressResult = readyToPublish()
        val viewModel = publicationViewModel()
        viewModel.onPatientMessageChange("Nos vemos en un mes")

        viewModel.events.test {
            viewModel.onBack()
            assertEquals(ConsultationStepEvent.Back, awaitItem())
        }
        assertEquals("Nos vemos en un mes", consultations.drafts.single().patientMessage?.text)
    }

    @Test
    fun `EV-5 publication that finds the consultation closed returns to the patient`() = runTest {
        consultations.inProgressResult = readyToPublish()
        consultations.publishResults = listOf(failureOf(DomainError.Conflict("ConsultationNotInProgress")))
        val viewModel = publicationViewModel()

        viewModel.events.test {
            viewModel.onPublish()
            assertEquals(ConsultationStepEvent.ConsultationClosed, awaitItem())
        }
        assertFalse(viewModel.state.value.publishFailed)
    }
}
