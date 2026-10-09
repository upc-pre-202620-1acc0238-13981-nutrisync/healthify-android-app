package pe.edu.upc.healthify.features.iam.infrastructure.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.network.auth.FakeSessionTokenStore
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserDao
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserEntity
import pe.edu.upc.healthify.features.iam.infrastructure.mapper.toEntity
import pe.edu.upc.healthify.features.iam.infrastructure.remote.SessionService
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.ChangePreferredLanguageRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.UserDto
import pe.edu.upc.healthify.testing.sessionUser
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AccountRepositoryImplTest {

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

    private class FakeSessionService : SessionService {
        var user: () -> UserDto = { UserDto(userId = 12, email = "maria@mail.com", role = "Patient", createdAt = CREATED_AT) }
        var calls = 0
        override suspend fun signOut() = Unit
        override suspend fun getNavigationShell(sessionId: Long): NavigationShellDto = error("not used")
        override suspend fun getUser(userId: Long): UserDto {
            calls++
            return user()
        }
        override suspend fun changePreferredLanguage(userId: Long, body: ChangePreferredLanguageRequestDto) = Unit
    }

    private val dao = FakeSessionUserDao()
    private val service = FakeSessionService()
    private val local = SessionLocalDataSource(FakeSessionTokenStore(), dao) {}

    // Lima (UTC−5): la cuenta creada el 8 a las 02:30 UTC es del 7 en el teléfono.
    private val clock = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"))
    private val repository = AccountRepositoryImpl(service, local, clock)

    @Test
    fun `reads the account date once, in the phone zone, and keeps it with the session`() = runTest {
        dao.upsert(sessionUser().toEntity())

        assertEquals(LocalDate.parse("2026-10-07"), repository.getCreatedOn(UserId(12)).getOrNull())
        assertEquals(LocalDate.parse("2026-10-07"), repository.getCreatedOn(UserId(12)).getOrNull())

        assertEquals(1, service.calls)
        assertEquals(CREATED_AT, dao.row.value?.createdAt)
    }

    @Test
    fun `signing in again as the same user keeps the saved date`() = runTest {
        dao.upsert(sessionUser().toEntity().copy(createdAt = CREATED_AT))

        local.save(sessionUser(), "a", "r")

        assertEquals(CREATED_AT, dao.row.value?.createdAt)
    }

    @Test
    fun `another user does not inherit the saved date`() = runTest {
        dao.upsert(sessionUser().toEntity().copy(createdAt = CREATED_AT))

        local.save(sessionUser(id = 99), "a", "r")

        assertEquals(null, dao.row.value?.createdAt)
    }

    @Test
    fun `offline without a saved date fails as network and saves nothing`() = runTest {
        dao.upsert(sessionUser().toEntity())
        service.user = { throw IOException("offline") }

        val result = repository.getCreatedOn(UserId(12))

        assertEquals(DomainError.Network, result.domainErrorOrNull())
        assertEquals(null, dao.row.value?.createdAt)
    }

    @Test
    fun `an account without a creation date is a malformed response`() = runTest {
        dao.upsert(sessionUser().toEntity())
        service.user = { UserDto(userId = 12, email = "maria@mail.com", role = "Patient", createdAt = null) }

        val result = repository.getCreatedOn(UserId(12))

        assertEquals(DomainError.Unexpected("MALFORMED_RESPONSE"), result.domainErrorOrNull())
    }

    private companion object {
        const val CREATED_AT = "2026-10-08T02:30:00+00:00"
    }
}
