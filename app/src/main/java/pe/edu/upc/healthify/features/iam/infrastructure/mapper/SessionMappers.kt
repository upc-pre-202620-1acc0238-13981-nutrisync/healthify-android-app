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

/** Rol del backend (`Patient`/`Practitioner`). Un rol desconocido es un contrato roto: falla rápido. */
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
 * @throws IllegalArgumentException si la respuesta no forma un usuario válido.
 */
fun SignInResponseDto.toDomain(): SessionUser {
    val email = EmailAddress(email)
    // DECISIÓN IAM-1: una cuenta sin nombres (anterior a IAM-1) usa la parte local del correo, como el backend.
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
 * @throws IllegalArgumentException si la fila guardada ya no forma un usuario válido.
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
