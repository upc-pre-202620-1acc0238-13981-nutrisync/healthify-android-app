package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.RegisterUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEmailError
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEvent
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpFieldErrors
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthenticationRepository()
    private val connectivity = FakeConnectivityObserver()
    private val viewModel by lazy { SignUpViewModel(RegisterUseCase(auth), connectivity) }

    private fun fillValidForm(role: UserRole? = UserRole.PATIENT) {
        viewModel.onGivenNamesChange("María")
        viewModel.onFamilyNamesChange("Flores")
        viewModel.onEmailChange("maria@correo.com")
        viewModel.onPasswordChange("Maria123!")
        role?.let(viewModel::onRoleSelect)
    }

    @Test
    fun `starts empty without a role (S3 Sin elegir)`() {
        val state = viewModel.state.value
        assertEquals(null, state.role)
        assertEquals(SignUpFieldErrors(), state.errors)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `invalid form shows every field error and does not call the backend (S3E)`() {
        viewModel.onEmailChange("maria.flores@")
        viewModel.onPasswordChange("maria123")

        viewModel.onSubmit()

        assertEquals(
            SignUpFieldErrors(
                givenNames = PersonName.Problem.REQUIRED,
                familyNames = PersonName.Problem.REQUIRED,
                email = SignUpEmailError.Invalid,
                weakPassword = true,
                roleMissing = true,
            ),
            viewModel.state.value.errors,
        )
        assertTrue(auth.signUps.isEmpty())
    }

    @Test
    fun `editing a field clears only its error`() {
        viewModel.onSubmit()

        viewModel.onEmailChange("m")
        viewModel.onRoleSelect(UserRole.PRACTITIONER)

        val errors = viewModel.state.value.errors
        assertEquals(null, errors.email)
        assertFalse(errors.roleMissing)
        assertTrue(errors.weakPassword)
    }

    @Test
    fun `valid form creates the account, signs in and goes to S5`() = runTest {
        fillValidForm(UserRole.PRACTITIONER)

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(SignUpEvent.NavigateToShellLoading, awaitItem())
        }
        assertEquals(UserRole.PRACTITIONER, auth.signUps.single().role)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `email already taken shows its message on the email field`() {
        auth.signUpResult = failureOf(DomainError.Conflict("EmailAlreadyTaken"))
        fillValidForm()

        viewModel.onSubmit()

        assertEquals(SignUpEmailError.AlreadyTaken, viewModel.state.value.errors.email)
    }

    @Test
    fun `backend validation codes map to their fields`() {
        mapOf(
            "InvalidEmail" to SignUpFieldErrors(email = SignUpEmailError.Invalid),
            "WeakPassword" to SignUpFieldErrors(weakPassword = true),
            "RoleNotDeclared" to SignUpFieldErrors(roleMissing = true),
            "InvalidRole" to SignUpFieldErrors(roleMissing = true),
            "NameRequired" to SignUpFieldErrors(givenNames = PersonName.Problem.REQUIRED),
        ).forEach { (code, expected) ->
            auth.signUpResult = failureOf(DomainError.Validation(code))
            fillValidForm()
            viewModel.onSubmit()
            assertEquals(code, expected, viewModel.state.value.errors)
        }
    }

    @Test
    fun `unexpected or network errors open the retry dialog and retry submits again`() {
        auth.signUpResult = failureOf(DomainError.Unexpected("InternalError"))
        fillValidForm()

        viewModel.onSubmit()
        assertTrue(viewModel.state.value.showErrorDialog)

        auth.signUpResult = failureOf(DomainError.Network)
        viewModel.onErrorDialogRetry()
        assertTrue(viewModel.state.value.showErrorDialog)
        assertEquals(2, auth.signUps.size)

        viewModel.onErrorDialogDismiss()
        assertFalse(viewModel.state.value.showErrorDialog)
    }

    @Test
    fun `account created but sign-in failed goes to S4`() = runTest {
        auth.signInResult = failureOf(DomainError.Network)
        fillValidForm()

        viewModel.events.test {
            viewModel.onSubmit()
            assertEquals(SignUpEvent.NavigateToSignIn, awaitItem())
        }
    }

    @Test
    fun `offline shows the offline state and never submits`() {
        connectivity.online.value = false
        fillValidForm()

        viewModel.onSubmit()

        assertTrue(viewModel.state.value.isOffline)
        assertTrue(auth.signUps.isEmpty())

        connectivity.online.value = true
        assertFalse(viewModel.state.value.isOffline)
    }

    @Test
    fun `state toString never exposes the password`() {
        viewModel.onPasswordChange("Maria123!")
        assertFalse(viewModel.state.value.toString().contains("Maria123!"))
    }
}
