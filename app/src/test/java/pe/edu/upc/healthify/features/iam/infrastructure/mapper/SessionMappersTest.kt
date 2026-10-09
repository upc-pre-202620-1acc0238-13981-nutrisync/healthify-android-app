package pe.edu.upc.healthify.features.iam.infrastructure.mapper

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserEntity
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInResponseDto

class SessionMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val signInJson = """
        {
          "userId": 12, "email": "Maria.Lopez@Mail.com", "role": "Patient", "sessionId": 345,
          "token": "jwt", "givenNames": "María José", "familyNames": "López Díaz",
          "preferredLanguage": "en", "refreshToken": "rt", "expiresAt": "2026-10-08T10:00:00Z",
          "unknownField": true
        }
    """.trimIndent()

    @Test
    fun `sign-in response maps to the session user`() {
        val dto = json.decodeFromString<SignInResponseDto>(signInJson)

        val user = dto.toDomain()

        assertEquals(UserId(12), user.id)
        assertEquals("maria.lopez@mail.com", user.email.value)
        assertEquals(PersonName("María José", "López Díaz"), user.name)
        assertEquals("María", user.name.firstGivenName)
        assertEquals(UserRole.PATIENT, user.role)
        assertEquals(PreferredLanguage.ENGLISH, user.preferredLanguage)
        assertEquals(SessionId(345), user.sessionId)
    }

    @Test
    fun `account created before IAM-1 falls back to the email local part and spanish`() {
        val dto = SignInResponseDto(
            userId = 3, email = "nutri@clinic.pe", role = "Practitioner", sessionId = 9, token = "t",
            givenNames = null, familyNames = null, preferredLanguage = null, refreshToken = "r",
        )

        val user = dto.toDomain()

        assertEquals(PersonName("nutri", ""), user.name)
        assertEquals("nutri", user.name.fullName)
        assertEquals(UserRole.PRACTITIONER, user.role)
        assertEquals(PreferredLanguage.SPANISH, user.preferredLanguage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown role breaks the contract`() {
        SignInResponseDto(
            userId = 3, email = "a@b.pe", role = "Admin", sessionId = 9, token = "t", refreshToken = "r",
        ).toDomain()
    }

    @Test
    fun `entity round trip keeps every field`() {
        val user = SessionUser(
            id = UserId(12),
            email = EmailAddress("maria@mail.com"),
            name = PersonName("María", "López"),
            role = UserRole.PRACTITIONER,
            preferredLanguage = PreferredLanguage.ENGLISH,
            sessionId = SessionId(345),
        )

        val entity = user.toEntity()

        assertEquals(
            SessionUserEntity(
                userId = 12, email = "maria@mail.com", givenNames = "María", familyNames = "López",
                role = "Practitioner", preferredLanguage = "en", sessionId = 345,
            ),
            entity,
        )
        assertEquals(user, entity.toDomain())
    }
}
