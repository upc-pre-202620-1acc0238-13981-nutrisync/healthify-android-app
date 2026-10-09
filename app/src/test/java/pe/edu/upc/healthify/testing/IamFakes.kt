package pe.edu.upc.healthify.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.AccountRepository
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import java.time.LocalDate

/** `GET /users/{id}` → `CreatedAt`. Por defecto una cuenta antigua: no limita los días. */
class FakeAccountRepository(
    var createdOn: Result<LocalDate> = Result.success(LocalDate.parse("2025-01-01")),
) : AccountRepository {
    var calls = 0

    override suspend fun getCreatedOn(userId: UserId): Result<LocalDate> {
        calls++
        return createdOn
    }
}

fun sessionUser(role: UserRole = UserRole.PATIENT, id: Long = 12) = SessionUser(
    id = UserId(id),
    email = EmailAddress("maria@mail.com"),
    name = PersonName("María", "Flores"),
    role = role,
    preferredLanguage = PreferredLanguage.SPANISH,
    sessionId = SessionId(3),
)

class FakeConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}

/** Respuestas programables; registra lo que se pidió. */
class FakeAuthenticationRepository : AuthenticationRepository {
    var signUpResult: Result<Unit> = Result.success(Unit)
    var signInResult: Result<SessionUser> = Result.success(sessionUser())
    var signOutResult: Result<Unit> = Result.success(Unit)

    /** Una respuesta por llamada a `getNavigationShell`; la última se repite. */
    var navigationShellResults: List<Result<NavigationShell?>> = listOf(Result.success(NavigationShell.PATIENT))

    val signUps = mutableListOf<NewAccount>()
    val signIns = mutableListOf<Credentials>()
    var signOutCalls = 0
    var navigationShellCalls = 0

    override suspend fun signUp(account: NewAccount): Result<Unit> {
        signUps += account
        return signUpResult
    }

    override suspend fun signIn(credentials: Credentials): Result<SessionUser> {
        signIns += credentials
        return signInResult
    }

    override suspend fun signOutRemotely(): Result<Unit> {
        signOutCalls++
        return signOutResult
    }

    override suspend fun getNavigationShell(sessionId: SessionId): Result<NavigationShell?> {
        val result = navigationShellResults[minOf(navigationShellCalls, navigationShellResults.lastIndex)]
        navigationShellCalls++
        return result
    }
}

class FakeSessionRepository(user: SessionUser? = sessionUser(), loggedIn: Boolean = user != null) : SessionRepository {
    val user = MutableStateFlow(user)
    val loggedIn = MutableStateFlow(loggedIn)
    var logoutCalls = 0

    override val currentUser: Flow<SessionUser?> = this.user
    override val isLoggedIn: Flow<Boolean> = this.loggedIn
    override val sessionExpired: Flow<Unit> = emptyFlow()

    override suspend fun logoutLocally() {
        logoutCalls++
        user.value = null
        loggedIn.value = false
    }
}

fun <T> failureOf(error: DomainError): Result<T> = domainFailure(error)
