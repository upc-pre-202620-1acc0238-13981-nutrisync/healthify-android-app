package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.carerelationship.application.usecase.AcknowledgeActiveTargetsUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetActiveTargetsUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanVersionsUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MyPlanEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PlanChangeLine
import pe.edu.upc.healthify.testing.FakeActiveTargetsRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePlanVersionRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.activeTargets
import pe.edu.upc.healthify.testing.careLink
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.readStatus
import java.time.Instant

class MyPlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = FakeSessionRepository()
    private val targets = FakeActiveTargetsRepository()
    private val careLinks = FakeCareLinkRepository().apply {
        activeResult = Result.success(careLink(id = 8, isActive = true))
    }
    private val connectivity = FakeConnectivityObserver()

    private val viewModel by lazy {
        MyPlanViewModel(
            observeCurrentUser = ObserveCurrentUserUseCase(session),
            getActiveTargets = GetActiveTargetsUseCase(targets),
            getTargetsReadStatus = GetTargetsReadStatusUseCase(careLinks),
            acknowledgeActiveTargets = AcknowledgeActiveTargetsUseCase(careLinks),
            connectivityObserver = connectivity,
        )
    }

    @Test
    fun `PT4 translates codes and shows legacy restrictions as written`() {
        targets.result = Result.success(
            ActiveTargetsLookup(
                activeTargets(
                    restrictions = listOf(
                        DietaryRestriction.Catalog(RestrictionCode.SHELLFISH_FREE),
                        DietaryRestriction.Legacy("Sin ají"),
                        DietaryRestriction.Legacy("Nada de frituras los lunes"),
                    ),
                    patientMessage = "Probemos con estas ideas.",
                ),
            ),
        )

        val plan = requireNotNull(viewModel.state.value.plan)

        assertEquals(3, plan.planVersion)
        assertEquals(
            listOf(
                UiText.of(R.string.restriction_shellfish_free),
                UiText.Raw("Sin ají"),
                UiText.Raw("Nada de frituras los lunes"),
            ),
            plan.restrictions,
        )
        assertEquals(
            listOf(UiText.of(R.string.guideline_prioritize_vegetables), UiText.of(R.string.guideline_reduce_salt)),
            plan.guidelines,
        )
        assertEquals(
            listOf(
                PlanChangeLine(UiText.of(R.string.plan_change_guideline_added, UiText.of(R.string.guideline_reduce_salt)), false),
                PlanChangeLine(UiText.of(R.string.plan_change_no_target_changes), true),
            ),
            plan.changes,
        )
        assertEquals("Probemos con estas ideas.", plan.patientMessage)
        assertEquals(360f / 1850f, plan.proteinShare, 1e-4f)
    }

    @Test
    fun `offline PT4 shows the copy saved on the phone`() {
        val savedAt = Instant.parse("2026-10-07T08:10:00Z")
        connectivity.online.value = false
        targets.result = Result.success(ActiveTargetsLookup(activeTargets(), fromCache = true, savedAt = savedAt))

        val state = viewModel.state.value

        assertTrue(state.isOffline)
        assertEquals(savedAt, state.cachedAt)
        assertEquals(3, state.plan?.planVersion)
    }

    @Test
    fun `without targets PT4 shows the empty state and with an error the retry`() {
        targets.result = Result.success(ActiveTargetsLookup.None)
        assertTrue(viewModel.state.value.hasNoTargets)

        targets.result = failureOf(DomainError.Unexpected("HTTP_500"))
        viewModel.onRetry()
        assertTrue(viewModel.state.value.loadFailed)
        assertNull(viewModel.state.value.plan)
    }

    @Test
    fun `I have reviewed them acknowledges the shown version and goes back`() = runTest {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = 3))
        viewModel.state.value

        viewModel.events.test {
            viewModel.onAcknowledge()
            assertEquals(MyPlanEvent.NavigateBack, awaitItem())
        }
        assertEquals(listOf(CareLinkId(8) to 3), careLinks.acknowledgements)
    }

    @Test
    fun `with nothing pending it just goes back`() = runTest {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = null))
        viewModel.state.value

        viewModel.events.test {
            viewModel.onAcknowledge()
            assertEquals(MyPlanEvent.NavigateBack, awaitItem())
        }
        assertTrue(careLinks.acknowledgements.isEmpty())
    }

    @Test
    fun `offline it goes back without acknowledging`() = runTest {
        connectivity.online.value = false
        careLinks.readStatusResult = failureOf(DomainError.Network)
        targets.result = Result.success(ActiveTargetsLookup(activeTargets(), fromCache = true, savedAt = Instant.EPOCH))
        viewModel.state.value

        viewModel.events.test {
            viewModel.onAcknowledge()
            assertEquals(MyPlanEvent.NavigateBack, awaitItem())
        }
        assertTrue(careLinks.acknowledgements.isEmpty())
    }

    @Test
    fun `a failed acknowledgement shows the server error and can be retried`() = runTest {
        careLinks.readStatusResult = Result.success(readStatus(pendingVersion = 3))
        careLinks.acknowledgeResult = failureOf(DomainError.Unexpected("HTTP_500"))
        viewModel.onAcknowledge()
        assertTrue(viewModel.state.value.showServerError)
        assertFalse(viewModel.state.value.isAcknowledging)

        careLinks.acknowledgeResult = Result.success(Unit)
        viewModel.events.test {
            viewModel.onServerErrorRetry()
            assertEquals(MyPlanEvent.NavigateBack, awaitItem())
        }
        assertEquals(2, careLinks.acknowledgements.size)
    }

    @Test
    fun `PT4_1 lists the versions newest first`() = runTest {
        val repository = FakePlanVersionRepository().apply {
            result = Result.success(
                listOf(
                    PlanVersion(1, Instant.parse("2026-03-12T15:00:00Z"), isActive = false, energyKcal = 2000.0),
                    PlanVersion(3, Instant.parse("2026-09-04T15:00:00Z"), isActive = true, energyKcal = 1850.0),
                    PlanVersion(2, Instant.parse("2026-06-12T15:00:00Z"), isActive = false, energyKcal = 1900.0),
                ),
            )
        }
        val versions = PlanVersionsViewModel(ObserveCurrentUserUseCase(session), GetPlanVersionsUseCase(repository), connectivity)

        assertEquals(listOf(3, 2, 1), versions.state.value.versions.map { it.version })
        assertTrue(versions.state.value.versions.first().isActive)
    }

    @Test
    fun `PT4_1 offline shows the offline state instead of an error`() = runTest {
        val repository = FakePlanVersionRepository().apply { result = failureOf(DomainError.Network) }
        val versions = PlanVersionsViewModel(ObserveCurrentUserUseCase(session), GetPlanVersionsUseCase(repository), connectivity)

        assertTrue(versions.state.value.isOffline)
        assertFalse(versions.state.value.loadFailed)
    }
}
