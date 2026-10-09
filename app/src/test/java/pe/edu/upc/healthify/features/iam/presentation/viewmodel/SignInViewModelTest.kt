package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.SignInUseCase
import pe.edu.upc.healthify.features.iam.presentation.state.SignInError
import pe.edu.upc.healthify.features.iam.presentation.state.SignInEvent
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.sessionUser

class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthenticationRepository()
    private val connectivity = FakeConnectivityObserver()
    private val viewModel by lazy { SignInViewModel(SignInUseCase(auth), connectivity) }

    private fun fill(email: String = "maria@correo.com", password: String = "Maria123!") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun `successful sign-in goes to S5`() = runTest {
        fill()

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(SignInEvent.NavigateToShellLoading, awaitItem())
        }
        assertEquals("maria@correo.com", auth.signIns.single().email.value)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `InvalidCredentials shows S4_1`() {
        auth.signInResult = failureOf(DomainError.Unauthorized("InvalidCredentials"))
        fill()

        viewModel.onSubmit()

        assertEquals(SignInError.InvalidCredentials, viewModel.state.value.error)
    }

    @Test
    fun `AccountLocked shows S4_2 even though it is also a 401`() {
        auth.signInResult = failureOf(DomainError.Unauthorized("AccountLocked"))
        fill()

        viewModel.onSubmit()

        assertEquals(SignInError.AccountLocked, viewModel.state.value.error)
    }

    @Test
    fun `401 without code is read as incorrect credentials`() {
        auth.signInResult = failureOf(DomainError.Unauthorized())
        fill()

        viewModel.onSubmit()

        assertEquals(SignInError.InvalidCredentials, viewModel.state.value.error)
    }

    @Test
    fun `malformed email or empty password fail locally with the same message`() {
        fill(email = "maria")
        viewModel.onSubmit()
        assertEquals(SignInError.InvalidCredentials, viewModel.state.value.error)

        fill(password = "")
        viewModel.onSubmit()
        assertEquals(SignInError.InvalidCredentials, viewModel.state.value.error)

        assertTrue(auth.signIns.isEmpty())
    }

    @Test
    fun `editing clears the credentials error but keeps the lock notice`() {
        auth.signInResult = failureOf(DomainError.Unauthorized("InvalidCredentials"))
        fill()
        viewModel.onSubmit()
        viewModel.onPasswordChange("otra")
        assertNull(viewModel.state.value.error)

        auth.signInResult = failureOf(DomainError.Unauthorized("AccountLocked"))
        fill()
        viewModel.onSubmit()
        viewModel.onEmailChange("maria@correo.co")
        assertEquals(SignInError.AccountLocked, viewModel.state.value.error)
    }

    @Test
    fun `server error opens the retry dialog`() {
        auth.signInResult = failureOf(DomainError.Unexpected("InternalError"))
        fill()

        viewModel.onSubmit()
        assertTrue(viewModel.state.value.showErrorDialog)

        auth.signInResult = Result.success(sessionUser())
        viewModel.onErrorDialogRetry()
        assertFalse(viewModel.state.value.showErrorDialog)
        assertEquals(2, auth.signIns.size)
    }

    @Test
    fun `forgot password opens the coming soon dialog`() {
        viewModel.onForgotPasswordClick()
        assertTrue(viewModel.state.value.showForgotPasswordDialog)

        viewModel.onForgotPasswordDismiss()
        assertFalse(viewModel.state.value.showForgotPasswordDialog)
    }

    @Test
    fun `offline never calls the backend`() {
        connectivity.online.value = false
        fill()

        viewModel.onSubmit()

        assertTrue(viewModel.state.value.isOffline)
        assertTrue(auth.signIns.isEmpty())
    }

    @Test
    fun `create account goes to S3`() = runTest {
        viewModel.events.test {
            viewModel.onCreateAccountClick()
            assertEquals(SignInEvent.NavigateToSignUp, awaitItem())
        }
    }
}
