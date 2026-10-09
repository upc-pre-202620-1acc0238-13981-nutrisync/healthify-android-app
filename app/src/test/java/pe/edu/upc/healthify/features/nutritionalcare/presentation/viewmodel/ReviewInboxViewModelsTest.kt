package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AcceptPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AwaitPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanHistoryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.ResolveReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NutritionPlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.ReviewItemRoute
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ProposalSection
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewDialog
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewLoadError
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeNutritionPlanRepository
import pe.edu.upc.healthify.testing.FakePractitionerCareLinkRepository
import pe.edu.upc.healthify.testing.FakeReviewInboxRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.PATIENT_ID
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.planProposal
import pe.edu.upc.healthify.testing.reviewItem
import pe.edu.upc.healthify.testing.rosterPatient
import pe.edu.upc.healthify.testing.sessionUser
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewInboxViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val inbox = FakeReviewInboxRepository()
    private val connectivity = FakeConnectivityObserver()
    private val careLinks = FakePractitionerCareLinkRepository()
    private val plans = FakeNutritionPlanRepository()
    private val session = FakeSessionRepository(sessionUser(role = UserRole.PRACTITIONER, id = 3))

    private fun args() = SavedStateHandle(mapOf(ReviewItemRoute.ARG_REVIEW_ITEM_ID to 41L))

    private fun itemViewModel() = ReviewItemViewModel(
        savedStateHandle = args(),
        getOpenReviewItem = GetOpenReviewItemUseCase(inbox),
        awaitPlanProposal = AwaitPlanProposalUseCase(inbox),
        resolveReviewItem = ResolveReviewItemUseCase(inbox),
        acceptPlanProposal = AcceptPlanProposalUseCase(inbox),
        getPatientRoster = GetPatientRosterUseCase(careLinks),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        connectivityObserver = connectivity,
    )

    private fun adjustViewModel() = AdjustProposalViewModel(
        savedStateHandle = args(),
        getOpenReviewItem = GetOpenReviewItemUseCase(inbox),
        awaitPlanProposal = AwaitPlanProposalUseCase(inbox),
        getPlanHistory = GetPlanHistoryUseCase(plans),
        acceptPlanProposal = AcceptPlanProposalUseCase(inbox),
        connectivityObserver = connectivity,
    )

    // ----- PR13 -----

    @Test
    fun `PR13 lists the open items and is up to date when empty`() {
        val viewModel = ReviewInboxViewModel(GetOpenReviewItemsUseCase(inbox), connectivity)
        assertEquals(listOf(41L), viewModel.state.value.items?.map { it.id })
        assertEquals(listOf(ReviewItemState.OPEN), inbox.requestedStates)

        inbox.itemsResult = Result.success(emptyList())
        viewModel.onResume()
        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `PR13 offline without a previous read needs a connection`() {
        inbox.itemsResult = failureOf(DomainError.Network)
        val viewModel = ReviewInboxViewModel(GetOpenReviewItemsUseCase(inbox), connectivity)

        assertTrue(viewModel.state.value.needsConnection)
    }

    // ----- PR14 -----

    @Test
    fun `PR14 without an answer shows Responde Si o No and sends nothing`() {
        val viewModel = itemViewModel()

        viewModel.onResolve()

        assertTrue(viewModel.state.value.showOutcomeError)
        assertTrue(inbox.resolutions.isEmpty())
    }

    @Test
    fun `PR14 resolves with the answer and the note and goes back to the inbox`() = runTest {
        val viewModel = itemViewModel()
        assertEquals(ProposalSection.NotOffered, viewModel.state.value.proposal)

        viewModel.events.test {
            viewModel.onAdjustedChange(false)
            viewModel.onNoteChange("Se conversó en consulta")
            viewModel.onResolve()
            assertEquals(ReviewItemEvent.Resolved("Ana Flores"), awaitItem())
        }
        assertFalse(inbox.resolutions.single().resolvedWithAdjustment)
        assertEquals("Se conversó en consulta", inbox.resolutions.single().note)
    }

    @Test
    fun `PR14 a 404 means the item is no longer available`() {
        inbox.resolveResult = failureOf(DomainError.NotFound("ReviewItemNotFound"))
        val viewModel = itemViewModel()

        viewModel.onAdjustedChange(true)
        viewModel.onResolve()

        assertEquals(ReviewDialog.NOT_AVAILABLE, viewModel.state.value.dialog)
    }

    @Test
    fun `PR14 an item that is gone shows not available instead of the form`() {
        inbox.itemsResult = Result.success(emptyList())

        assertEquals(ReviewLoadError.NOT_AVAILABLE, itemViewModel().state.value.loadError)
    }

    @Test
    fun `PR14 Ajustar el plan ahora opens the patient record with the link from the roster`() = runTest {
        careLinks.rosterResult = Result.success(PatientRoster(listOf(rosterPatient(PATIENT_ID, "Ana Flores"))))
        val viewModel = itemViewModel()

        viewModel.events.test {
            viewModel.onAdjustPlanNow()
            assertEquals(ReviewItemEvent.OpenPatient(PATIENT_ID, PATIENT_ID + 100, "Ana Flores"), awaitItem())
        }
        assertTrue(inbox.resolutions.isEmpty())
    }

    // ----- PR14.IA: 202 → 200 y aceptación -----

    @Test
    fun `PR14 IA shows generating while the backend answers 202 and the proposal when it answers 200`() = runTest {
        inbox.itemsResult = Result.success(listOf(reviewItem(hasPlanProposal = true)))
        inbox.proposalResults = listOf(
            Result.success(PlanProposalLookup.Generating(5)),
            Result.success(PlanProposalLookup.Ready(planProposal())),
        )

        val viewModel = itemViewModel()
        runCurrent()
        assertEquals(ProposalSection.Generating, viewModel.state.value.proposal)
        assertFalse(viewModel.state.value.showsResolutionForm)

        advanceTimeBy(5_001)
        val proposal = viewModel.state.value.proposal as ProposalSection.Ready
        assertEquals(1650.0, proposal.proposal.proposed.energyKcal, 0.0)
        assertEquals(2, inbox.proposalCalls)
    }

    @Test
    fun `PR14 IA Resolver assigns the proposed plan as is`() = runTest {
        inbox.itemsResult = Result.success(listOf(reviewItem(hasPlanProposal = true)))
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.Ready(planProposal())))
        val viewModel = itemViewModel()
        runCurrent()

        viewModel.events.test {
            viewModel.onAcceptAsIs()
            assertEquals(ReviewItemEvent.Resolved("Ana Flores"), awaitItem())
        }
        assertEquals(listOf<PlanAcceptance>(PlanAcceptance.AsIs), inbox.acceptances)
        assertTrue(inbox.resolutions.isEmpty())
    }

    @Test
    fun `PR14 IA a 404 proposal falls back to PR14 and resolving without the plan uses the resolution`() = runTest {
        inbox.itemsResult = Result.success(listOf(reviewItem(hasPlanProposal = true)))
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.None))
        val viewModel = itemViewModel()
        runCurrent()

        assertEquals(ProposalSection.NotOffered, viewModel.state.value.proposal)
        assertTrue(viewModel.state.value.showsResolutionForm)
    }

    @Test
    fun `PR14 IA resolving without assigning shows the form and never accepts`() = runTest {
        inbox.itemsResult = Result.success(listOf(reviewItem(hasPlanProposal = true)))
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.Ready(planProposal())))
        val viewModel = itemViewModel()
        runCurrent()

        viewModel.onResolveWithoutProposal()
        assertTrue(viewModel.state.value.showsResolutionForm)
        viewModel.onAdjustedChange(false)
        viewModel.onResolve()

        assertTrue(inbox.acceptances.isEmpty())
        assertEquals(1, inbox.resolutions.size)
    }

    @Test
    fun `PR14 IA an already decided proposal says it was resolved`() = runTest {
        inbox.itemsResult = Result.success(listOf(reviewItem(hasPlanProposal = true)))
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.Ready(planProposal())))
        inbox.acceptResult = failureOf(DomainError.Conflict("PlanProposalAlreadyDecided"))
        val viewModel = itemViewModel()
        runCurrent()

        viewModel.onAcceptAsIs()

        assertEquals(ReviewDialog.ALREADY_RESOLVED, viewModel.state.value.dialog)
    }

    // ----- PR14.IA-A -----

    private fun currentPlan(vararg codes: PlanGuidelineCode) = PlanHistory(
        listOf(
            NutritionPlanVersion(
                version = 3,
                isActive = true,
                publishedAt = Instant.parse("2026-08-01T15:00:00Z"),
                targets = Targets(1796.0, 119.0, 195.0, 60.0),
                guidelines = codes.map { PlanGuidelineItem.Catalog(it) } + PlanGuidelineItem.Custom("Camina 20 minutos"),
                restrictions = emptyList(),
                patientMessage = null,
            ),
        ),
    )

    @Test
    fun `PR14 IA-A comes prefilled with the proposal and the resulting guidelines`() = runTest {
        inbox.proposalResults = listOf(
            Result.success(PlanProposalLookup.Ready(planProposal(removed = listOf(PlanGuidelineCode.REDUCE_SALT)))),
        )
        plans.result = Result.success(currentPlan(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.REDUCE_SALT))

        val state = adjustViewModel().state.value

        assertFalse(state.isLoading)
        assertEquals("1650", state.energy)
        assertEquals("119", state.protein)
        assertEquals(1796.0, state.previousEnergyKcal!!, 0.0)
        assertEquals("Ana", state.patientFirstName)
        assertEquals(
            setOf(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER),
            state.guidelines,
        )
    }

    @Test
    fun `PR14 IA-A assigns the plan with the edits`() = runTest {
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.Ready(planProposal())))
        plans.result = Result.success(currentPlan(PlanGuidelineCode.PRIORITIZE_VEGETABLES))
        val viewModel = adjustViewModel()

        viewModel.events.test {
            viewModel.onEnergyChange("1700")
            viewModel.onGuidelineToggle(PlanGuidelineCode.PRIORITIZE_VEGETABLES, false)
            viewModel.onAssign()
            assertEquals(AdjustProposalEvent.Assigned("Ana Flores"), awaitItem())
        }
        val edits = (inbox.acceptances.single() as PlanAcceptance.WithEdits).edits
        assertEquals(1700.0, edits.targets.energyKcal, 0.0)
        assertEquals(setOf(PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER), edits.guidelines)
        assertEquals("Notamos que tus cenas son más ligeras.", edits.patientMessage?.text)
    }

    @Test
    fun `PR14 IA-A below the calorie floor marks the energy and invalid targets are not sent`() = runTest {
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.Ready(planProposal())))
        plans.result = Result.success(currentPlan())
        inbox.acceptResult = failureOf(DomainError.Validation("PlanProposalOutOfSafetyBounds"))
        val viewModel = adjustViewModel()

        viewModel.onProteinChange("0")
        viewModel.onAssign()
        assertEquals(setOf(ProposalEditField.PROTEIN), viewModel.state.value.invalidFields)
        assertTrue(inbox.acceptances.isEmpty())

        viewModel.onProteinChange("110")
        viewModel.onEnergyChange("900")
        viewModel.onAssign()
        assertTrue(viewModel.state.value.belowSafetyFloor)
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun `PR14 IA-A without the proposal any more is not available`() = runTest {
        inbox.proposalResults = listOf(Result.success(PlanProposalLookup.None))

        assertEquals(ReviewLoadError.NOT_AVAILABLE, adjustViewModel().state.value.loadError)
    }
}
