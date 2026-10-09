package pe.edu.upc.healthify.features.iam.infrastructure.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.network.auth.FakeSessionTokenStore
import pe.edu.upc.healthify.core.network.auth.SessionExpiryNotifier
import pe.edu.upc.healthify.features.iam.application.usecase.LogoutLocallyUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveIsLoggedInUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveSessionExpiredUseCase
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserDao
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserEntity

class SessionRepositoryImplTest {

    private class FakeSessionUserDao : SessionUserDao {
        val row = MutableStateFlow<SessionUserEntity?>(null)
        override fun observe(): Flow<SessionUserEntity?> = row
        override suspend fun get(): SessionUserEntity? = row.value
        override suspend fun upsert(user: SessionUserEntity) {
            row.value = user
        }
        override suspend fun clear() {
            row.value = null
        }
    }

    private val tokenStore = FakeSessionTokenStore()
    private val dao = FakeSessionUserDao()
    private var syncRequests = 0
    private val local = SessionLocalDataSource(tokenStore, dao) { syncRequests++ }
    private val notifier = SessionExpiryNotifier()
    private val repository = SessionRepositoryImpl(local, notifier)

    private val patient = SessionUser(
        id = UserId(12),
        email = EmailAddress("maria@mail.com"),
        name = PersonName("María", "López"),
        role = UserRole.PATIENT,
        preferredLanguage = PreferredLanguage.SPANISH,
        sessionId = SessionId(3),
    )

    @Test
    fun `starts logged out`() = runTest {
        assertFalse(ObserveIsLoggedInUseCase(repository)().first())
        assertNull(ObserveCurrentUserUseCase(repository)().first())
        assertNull(local.activeUserId())
    }

    @Test
    fun `saved session exposes the user, logs in and asks to sync pending operations`() = runTest {
        local.save(patient, accessToken = "a", refreshToken = "r")

        assertTrue(repository.isLoggedIn.first())
        assertEquals(patient, repository.currentUser.first())
        assertEquals(12L, local.activeUserId())
        assertEquals("a", tokenStore.currentAccessToken)
        assertEquals(1, syncRequests)
    }

    @Test
    fun `expired tokens log out but keep the user for S6`() = runTest {
        local.save(patient, "a", "r")

        repository.isLoggedIn.test {
            assertTrue(awaitItem())
            tokenStore.clear()
            assertFalse(awaitItem())
        }
        assertEquals(UserRole.PATIENT, repository.currentUser.first()?.role)
        assertNull(local.activeUserId())
    }

    @Test
    fun `logout locally clears tokens and user`() = runTest {
        local.save(patient, "a", "r")

        LogoutLocallyUseCase(repository)()

        assertFalse(repository.isLoggedIn.first())
        assertNull(repository.currentUser.first())
        assertNull(tokenStore.currentRefreshToken)
    }

    @Test
    fun `corrupt stored user reads as no user`() = runTest {
        dao.upsert(
            SessionUserEntity(
                userId = 12, email = "x", givenNames = "A", familyNames = "", role = "Patient",
                preferredLanguage = "es", sessionId = 3,
            ),
        )

        assertNull(repository.currentUser.first())
    }

    @Test
    fun `session expired events reach the use case`() = runTest {
        ObserveSessionExpiredUseCase(repository)().test {
            notifier.notifyExpired()
            awaitItem()
        }
    }
}
