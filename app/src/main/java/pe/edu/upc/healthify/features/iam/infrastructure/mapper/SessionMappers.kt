package pe.edu.upc.healthify.features.iam.infrastructure.mapper

import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserEntity
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInResponseDto

private const val ROLE_PATIENT = "Patient"
private const val ROLE_PRACTITIONER = "Practitioner"

/** Backend role (`Patient`/`Practitioner`). An unknown role is a broken contract: fail fast. */
internal fun String.toUserRole(): UserRole = when {
    equals(ROLE_PATIENT, ignoreCase = true) -> UserRole.PATIENT
    equals(ROLE_PRACTITIONER, ignoreCase = true) -> UserRole.PRACTITIONER
    else -> throw IllegalArgumentException("Unknown role: $this")
}

internal fun UserRole.toCode(): String = when (this) {
    UserRole.PATIENT -> ROLE_PATIENT
    UserRole.PRACTITIONER -> ROLE_PRACTITIONER
}

/**
 * @throws IllegalArgumentException if the response does not form a valid user.
 */
fun SignInResponseDto.toDomain(): SessionUser {
    val email = EmailAddress(email)
    // An account without names (created before names were required) uses the email local part, like the backend.
    val given = givenNames?.takeIf { it.isNotBlank() } ?: email.value.substringBefore('@')
    return SessionUser(
        id = UserId(userId),
        email = email,
        name = PersonName.of(given, familyNames.orEmpty()),
        role = role.toUserRole(),
        preferredLanguage = PreferredLanguage.fromCode(preferredLanguage),
        sessionId = SessionId(sessionId),
    )
}

/**
 * @throws IllegalArgumentException if the stored row no longer forms a valid user.
 */
fun SessionUserEntity.toDomain(): SessionUser = SessionUser(
    id = UserId(userId),
    email = EmailAddress(email),
    name = PersonName(givenNames, familyNames),
    role = role.toUserRole(),
    preferredLanguage = PreferredLanguage.fromCode(preferredLanguage),
    sessionId = SessionId(sessionId),
)

fun SessionUser.toEntity(): SessionUserEntity = SessionUserEntity(
    userId = id.value,
    email = email.value,
    givenNames = name.givenNames,
    familyNames = name.familyNames,
    role = role.toCode(),
    preferredLanguage = preferredLanguage.code,
    sessionId = sessionId.value,
)
