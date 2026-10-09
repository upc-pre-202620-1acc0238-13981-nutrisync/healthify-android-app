package pe.edu.upc.healthify.features.onboarding.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveIsLoggedInUseCase
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashEvent
import pe.edu.upc.healthify.features.onboarding.presentation.state.WelcomeEvent
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.sessionUser

class OnboardingViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private suspend fun splashDestination(session: FakeSessionRepository): SplashDestination {
        val viewModel = SplashViewModel(ObserveCurrentUserUseCase(session), ObserveIsLoggedInUseCase(session))
        var destination: SplashDestination? = null
        viewModel.events.test {
            destination = (awaitItem() as SplashEvent.Navigate).destination
        }
        return destination!!
    }

    @Test
    fun `splash without session goes to S2`() = runTest {
        assertEquals(SplashDestination.Welcome, splashDestination(FakeSessionRepository(user = null)))
    }

    @Test
    fun `splash with session goes to S5`() = runTest {
        assertEquals(SplashDestination.ShellLoading, splashDestination(FakeSessionRepository(sessionUser())))
    }

    @Test
    fun `splash with a saved user whose tokens expired goes to S6`() = runTest {
        val session = FakeSessionRepository(sessionUser(), loggedIn = false)
        assertEquals(SplashDestination.SessionExpired, splashDestination(session))
    }

    @Test
    fun `welcome navigates when online`() = runTest {
        val viewModel = WelcomeViewModel(FakeConnectivityObserver(online = true))

        viewModel.events.test {
            viewModel.onCreateAccount()
            assertEquals(WelcomeEvent.NavigateToSignUp, awaitItem())
            viewModel.onHaveAccount()
            assertEquals(WelcomeEvent.NavigateToSignIn, awaitItem())
        }
    }

    @Test
    fun `welcome shows the banner instead of navigating when offline, and hides it when back online`() = runTest {
        val connectivity = FakeConnectivityObserver(online = false)
        val viewModel = WelcomeViewModel(connectivity)
        assertFalse(viewModel.state.value.showOfflineBanner)

        viewModel.events.test {
            viewModel.onCreateAccount()
            assertTrue(viewModel.state.value.showOfflineBanner)
            expectNoEvents()
        }

        connectivity.online.value = true
        assertFalse(viewModel.state.value.showOfflineBanner)
    }
}
