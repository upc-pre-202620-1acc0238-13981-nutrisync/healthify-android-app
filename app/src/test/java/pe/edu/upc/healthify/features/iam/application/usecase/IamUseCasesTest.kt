package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpValidation
import pe.edu.upc.healthify.features.iam.domain.entity.validateNewAccount
import pe.edu.upc.healthify.features.iam.domain.repository.UserRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.sessionUser

class IamUseCasesTest {

    private val auth = FakeAuthenticationRepository()
    private val account = (
        validateNewAccount("María", "Flores", "maria@correo.com", "Maria123!", UserRole.PATIENT)
            as SignUpValidation.Valid
        ).account

    @Test
    fun `register signs up and then signs in with the same credentials`() = runTest {
        val result = RegisterUseCase(auth)(account)

        assertTrue(result is RegisterResult.SignedIn)
        assertEquals(listOf(account), auth.signUps)
        assertEquals("maria@correo.com", auth.signIns.single().email.value)
        assertEquals("Maria123!", auth.signIns.single().password)
    }

    @Test
    fun `register stops when sign-up fails`() = runTest {
        auth.signUpResult = failureOf(DomainError.Conflict("EmailAlreadyTaken"))

        val result = RegisterUseCase(auth)(account)

        assertEquals(RegisterResult.Failed(DomainError.Conflict("EmailAlreadyTaken")), result)
        assertTrue(auth.signIns.isEmpty())
    }

    @Test
    fun `register reports a created account when the automatic sign-in fails`() = runTest {
        auth.signInResult = failureOf(DomainError.Network)

        assertEquals(RegisterResult.CreatedWithoutSession(DomainError.Network), RegisterUseCase(auth)(account))
    }

    @Test
    fun `sign out clears the local session even if the backend does not answer`() = runTest {
        val session = FakeSessionRepository()
        auth.signOutResult = failureOf(DomainError.Network)

        SignOutUseCase(auth, session)()

        assertEquals(1, auth.signOutCalls)
        assertEquals(1, session.logoutCalls)
    }

    @Test
    fun `navigation shell comes from the backend`() = runTest {
        auth.navigationShellResults = listOf(Result.success(NavigationShell.PRACTITIONER))

        val shell = ResolveNavigationShellUseCase(auth)(sessionUser(UserRole.PATIENT)).getOrThrow()

        assertEquals(NavigationShell.PRACTITIONER, shell)
    }

    @Test
    fun `navigation shell falls back to the role when missing, offline or not found`() = runTest {
        listOf(
            Result.success(null),
            failureOf(DomainError.Network),
            failureOf(DomainError.NotFound("SessionNotFound")),
        ).forEach { response ->
            auth.navigationShellResults = listOf(response)
            val shell = ResolveNavigationShellUseCase(auth)(sessionUser(UserRole.PRACTITIONER)).getOrThrow()
            assertEquals(NavigationShell.PRACTITIONER, shell)
        }
    }

    @Test
    fun `navigation shell propagates server errors`() = runTest {
        auth.navigationShellResults = listOf(failureOf(DomainError.Unexpected("InternalError")))

        val result = ResolveNavigationShellUseCase(auth)(sessionUser())

        assertEquals(DomainError.Unexpected("InternalError"), result.domainErrorOrNull())
    }

    @Test
    fun `change preferred language uses the signed-in user and skips the call when unchanged`() = runTest {
        val calls = mutableListOf<Pair<UserId, PreferredLanguage>>()
        val users = object : UserRepository {
            override suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage): Result<Unit> {
                calls += userId to language
                return Result.success(Unit)
            }
        }
        val useCase = ChangePreferredLanguageUseCase(FakeSessionRepository(sessionUser(id = 7)), users)

        useCase(PreferredLanguage.SPANISH)
        useCase(PreferredLanguage.ENGLISH)

        assertEquals(listOf(UserId(7) to PreferredLanguage.ENGLISH), calls)
    }

    @Test
    fun `change preferred language without session is unauthorized`() = runTest {
        val users = object : UserRepository {
            override suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage) =
                Result.success(Unit)
        }

        val result = ChangePreferredLanguageUseCase(FakeSessionRepository(user = null), users)(PreferredLanguage.ENGLISH)

        assertEquals(DomainError.Unauthorized(), result.domainErrorOrNull())
    }
}
