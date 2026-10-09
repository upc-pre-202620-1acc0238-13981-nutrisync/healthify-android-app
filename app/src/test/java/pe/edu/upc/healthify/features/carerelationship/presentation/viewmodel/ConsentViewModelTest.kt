package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GrantConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentEvent
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf

class ConsentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakeCareLinkRepository()
    private val connectivity = FakeConnectivityObserver()
    private val viewModel by lazy {
        ConsentViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ConsentRoute.ARG_CARE_LINK_ID to 8L)),
            observeCurrentUser = ObserveCurrentUserUseCase(FakeSessionRepository()),
            grantConsent = GrantConsentUseCase(careLinks),
            connectivityObserver = connectivity,
        )
    }

    @Test
    fun `AI starts off because it is a separate explicit consent`() {
        assertFalse(viewModel.state.value.aiProcessingGranted)
    }

    @Test
    fun `consent without AI activates the link and goes to PT3`() = runTest {
        viewModel.events.test {
            viewModel.onGrantConsent()
            assertEquals(ConsentEvent.NavigateToHome, awaitItem())
        }
        val grant = careLinks.grants.single()
        assertEquals(CareLinkId(8), grant.careLinkId)
        assertEquals(ConsentScope.CURRENT, grant.scope)
        assertFalse(grant.aiProcessingGranted)
    }

    @Test
    fun `consent with AI sends aiProcessingGranted`() = runTest {
        viewModel.onAiProcessingChange(true)

        viewModel.events.test {
            viewModel.onGrantConsent()
            assertEquals(ConsentEvent.NavigateToHome, awaitItem())
        }
        assertTrue(careLinks.grants.single().aiProcessingGranted)
        assertEquals(ConsentScope.CURRENT, careLinks.grants.single().scope)
    }

    @Test
    fun `turning AI back off before confirming sends false`() {
        viewModel.onAiProcessingChange(true)
        viewModel.onAiProcessingChange(false)

        viewModel.onGrantConsent()

        assertFalse(careLinks.grants.single().aiProcessingGranted)
    }

    @Test
    fun `not now leaves the link pending without granting anything`() = runTest {
        viewModel.events.test {
            viewModel.onNotNow()
            assertEquals(ConsentEvent.NavigateToPendingConsent, awaitItem())
        }
        assertTrue(careLinks.grants.isEmpty())
    }

    @Test
    fun `offline the button does nothing`() {
        connectivity.online.value = false

        viewModel.onGrantConsent()

        assertTrue(viewModel.state.value.isOffline)
        assertTrue(careLinks.grants.isEmpty())
    }

    @Test
    fun `each PT2 code shows its notice and the notice leads to the right place`() = runTest {
        val expected = listOf(
            Triple(DomainError.Conflict("ConsentAlreadyGranted"), ConsentError.AlreadyGranted, ConsentEvent.NavigateToHome),
            Triple(
                DomainError.Conflict("DischargedLinkCannotBeReactivated"),
                ConsentError.LinkDischarged,
                ConsentEvent.NavigateToScanInvitation,
            ),
            Triple(
                DomainError.Conflict("CareLinkAlreadyRevoked"),
                ConsentError.LinkRevoked,
                ConsentEvent.NavigateToScanInvitation,
            ),
        )
        expected.forEach { (error, notice, event) ->
            careLinks.grantResult = failureOf(error)
            viewModel.onGrantConsent()
            assertEquals(notice, viewModel.state.value.error)

            viewModel.events.test {
                viewModel.onErrorAction()
                assertEquals(event, awaitItem())
            }
            assertNull(viewModel.state.value.error)
        }
    }

    @Test
    fun `scope required notice just closes`() {
        careLinks.grantResult = failureOf(DomainError.Validation("ConsentScopeRequired"))
        viewModel.onGrantConsent()
        assertEquals(ConsentError.ScopeRequired, viewModel.state.value.error)

        viewModel.onErrorAction()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `link not found or server failure shows the server error and can retry`() = runTest {
        careLinks.grantResult = failureOf(DomainError.NotFound("CareLinkNotFound"))
        viewModel.onGrantConsent()
        assertTrue(viewModel.state.value.showServerError)

        careLinks.grantResult = null
        viewModel.events.test {
            viewModel.onServerErrorRetry()
            assertEquals(ConsentEvent.NavigateToHome, awaitItem())
        }
        assertEquals(2, careLinks.grants.size)
        assertFalse(viewModel.state.value.showServerError)
    }
}
