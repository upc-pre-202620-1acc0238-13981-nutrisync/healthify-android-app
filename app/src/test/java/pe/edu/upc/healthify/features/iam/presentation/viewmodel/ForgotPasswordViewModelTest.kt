package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.iam.application.usecase.RequestPasswordResetUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ResetPasswordUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.VerifyPasswordResetCodeUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetCode
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordEvent
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordStep
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthenticationRepository()
    private val connectivity = FakeConnectivityObserver()
    private val viewModel by lazy {
        ForgotPasswordViewModel(
            RequestPasswordResetUseCase(auth),
            VerifyPasswordResetCodeUseCase(auth),
            ResetPasswordUseCase(auth),
            connectivity,
        )
    }

    private fun toCodeStep(email: String = " Maria@Correo.com ") {
        viewModel.onEmailChange(email)
        viewModel.onSendCode()
    }

    private fun toNewPasswordStep(code: String = "123456") {
        toCodeStep()
        viewModel.onCodeChange(code)
        viewModel.onVerifyCode()
    }

    private fun submitPassword(password: String = "Nueva#2026") {
        viewModel.onNewPasswordChange(password)
        viewModel.onSubmitNewPassword()
    }

    // Paso 1 · correo

    @Test
    fun `a malformed email is not sent`() {
        viewModel.onEmailChange("maria@")
        viewModel.onSendCode()

        assertTrue(viewModel.state.value.invalidEmail)
        assertEquals(ForgotPasswordStep.Email, viewModel.state.value.step)
        assertTrue(auth.passwordResetRequests.isEmpty())
    }

    @Test
    fun `sending the code moves to the code step with the normalized email`() {
        toCodeStep()

        assertEquals("maria@correo.com", auth.passwordResetRequests.single().value)
        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)
        assertEquals(ForgotPasswordViewModel.RESEND_COOLDOWN_SECONDS, viewModel.state.value.resendInSeconds)
    }

    @Test
    fun `offline never calls the backend`() {
        connectivity.online.value = false
        viewModel.onEmailChange("maria@correo.com")

        viewModel.onSendCode()

        assertTrue(auth.passwordResetRequests.isEmpty())
    }

    // Paso 2 · código

    @Test
    fun `resend waits for the countdown`() = runTest {
        toCodeStep()

        viewModel.onResendCode()
        assertEquals(1, auth.passwordResetRequests.size)

        advanceTimeBy(ForgotPasswordViewModel.RESEND_COOLDOWN_SECONDS * 1_000L + 1)
        assertEquals(0, viewModel.state.value.resendInSeconds)

        viewModel.onResendCode()
        assertEquals(2, auth.passwordResetRequests.size)
        assertTrue(viewModel.state.value.codeResent)
    }

    @Test
    fun `the code field keeps six digits only`() {
        toCodeStep()

        viewModel.onCodeChange("12 34-5678")

        assertEquals("123456", viewModel.state.value.code)
    }

    @Test
    fun `an incomplete code never reaches the backend`() {
        toCodeStep()
        viewModel.onCodeChange("123")

        viewModel.onVerifyCode()

        assertTrue(viewModel.state.value.invalidCode)
        assertTrue(auth.codeVerifications.isEmpty())
    }

    @Test
    fun `a right code moves to the new password step`() {
        toNewPasswordStep()

        val (email, code) = auth.codeVerifications.single()
        assertEquals("maria@correo.com", email.value)
        assertEquals(PasswordResetCode.parse("123456"), code)
        assertEquals(ForgotPasswordStep.NewPassword, viewModel.state.value.step)
        assertEquals("", viewModel.state.value.code)
    }

    @Test
    fun `PasswordResetCodeInvalid marks the code and stays on the step`() {
        auth.verifyPasswordResetCodeResult = failureOf(DomainError.Validation("PasswordResetCodeInvalid"))

        toNewPasswordStep()

        assertTrue(viewModel.state.value.invalidCode)
        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)
        assertFalse(viewModel.state.value.showErrorDialog)
    }

    // Paso 3 · contraseña nueva

    @Test
    fun `a weak password never reaches the backend`() {
        toNewPasswordStep()

        submitPassword("corta")

        assertTrue(viewModel.state.value.weakPassword)
        assertTrue(auth.passwordResets.isEmpty())
    }

    @Test
    fun `the token from the verification sets the password and finishes the flow`() {
        auth.verifyPasswordResetCodeResult = Result.success(PasswordResetToken("token-xyz"))
        toNewPasswordStep()

        submitPassword()

        val reset = auth.passwordResets.single()
        assertEquals("token-xyz", reset.token.value)
        assertEquals("Nueva#2026", reset.newPassword.value)
        assertEquals(ForgotPasswordStep.Done, viewModel.state.value.step)
        assertEquals("", viewModel.state.value.newPassword)
    }

    @Test
    fun `PasswordReuseNotAllowed marks the password and keeps the token`() {
        auth.resetPasswordResult = failureOf(DomainError.Validation("PasswordReuseNotAllowed"))
        toNewPasswordStep()

        submitPassword("Secreta#2026")
        assertTrue(viewModel.state.value.passwordReused)
        assertEquals(ForgotPasswordStep.NewPassword, viewModel.state.value.step)

        viewModel.onNewPasswordChange("Otra#2026x")
        assertFalse(viewModel.state.value.passwordReused)
        auth.resetPasswordResult = Result.success(Unit)
        viewModel.onSubmitNewPassword()

        assertEquals(2, auth.passwordResets.size)
        assertEquals(auth.passwordResets[0].token, auth.passwordResets[1].token)
        assertEquals(ForgotPasswordStep.Done, viewModel.state.value.step)
    }

    @Test
    fun `WeakPassword from the backend marks the password`() {
        auth.resetPasswordResult = failureOf(DomainError.Validation("WeakPassword"))
        toNewPasswordStep()

        submitPassword()

        assertTrue(viewModel.state.value.weakPassword)
    }

    @Test
    fun `PasswordResetTokenInvalid goes back to the code step with a notice`() {
        auth.resetPasswordResult = failureOf(DomainError.Validation("PasswordResetTokenInvalid"))
        toNewPasswordStep()

        submitPassword()

        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)
        assertTrue(viewModel.state.value.tokenExpired)
        assertEquals("", viewModel.state.value.newPassword)
    }

    // Errores y navegación

    @Test
    fun `an unexpected error opens the dialog and retry repeats the failed call`() {
        auth.verifyPasswordResetCodeResult = failureOf(DomainError.Network)
        toNewPasswordStep()
        assertTrue(viewModel.state.value.showErrorDialog)
        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)

        auth.verifyPasswordResetCodeResult = Result.success(PasswordResetToken("token-1"))
        viewModel.onErrorDialogRetry()

        assertEquals(2, auth.codeVerifications.size)
        assertFalse(viewModel.state.value.showErrorDialog)
        assertEquals(ForgotPasswordStep.NewPassword, viewModel.state.value.step)
    }

    @Test
    fun `back walks new password, code, email and then S4`() = runTest {
        toNewPasswordStep()
        viewModel.onNewPasswordChange("Nueva#2026")

        viewModel.onBack()
        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)
        assertEquals("", viewModel.state.value.newPassword)
        assertFalse(viewModel.state.value.tokenExpired)

        viewModel.onBack()
        assertEquals(ForgotPasswordStep.Email, viewModel.state.value.step)
        assertEquals("maria@correo.com", viewModel.state.value.email.trim().lowercase())

        viewModel.events.test {
            viewModel.onBack()
            assertEquals(ForgotPasswordEvent.NavigateToSignIn, awaitItem())
        }
    }

    @Test
    fun `going back from the new password step discards the token`() {
        toNewPasswordStep()
        viewModel.onBack()

        // Sin token, «Cambiar contraseña» no llega al backend: vuelve al paso del código.
        viewModel.onSubmitNewPassword()

        assertTrue(auth.passwordResets.isEmpty())
        assertEquals(ForgotPasswordStep.Code, viewModel.state.value.step)
    }
}
