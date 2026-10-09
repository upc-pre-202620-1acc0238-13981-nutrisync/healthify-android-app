package pe.edu.upc.healthify.features.iam.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `SignInResource(Email, Password)` — sin `role` (login único, IAM-5). */
@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String,
)

/**
 * `SignInResponseResource(UserId, Email, Role, SessionId, Token, GivenNames, FamilyNames, PreferredLanguage,
 * RefreshToken, ExpiresAt)` (F2, IAM-1/3/4).
 */
@Serializable
data class SignInResponseDto(
    val userId: Long,
    val email: String,
    val role: String,
    val sessionId: Long,
    val token: String,
    val givenNames: String? = null,
    val familyNames: String? = null,
    val preferredLanguage: String? = null,
    val refreshToken: String,
    val expiresAt: String? = null,
)

/** Cuerpo de `POST /authentication/token-refreshes` (IAM-4). */
@Serializable
data class TokenRefreshRequestDto(
    val refreshToken: String,
)

/** `200` de `POST /authentication/token-refreshes`: `{ token, refreshToken, expiresAt }`. */
@Serializable
data class TokenRefreshResponseDto(
    val token: String,
    val refreshToken: String,
    val expiresAt: String? = null,
)

/** `SignUpResource(Email, Password, Role, GivenNames, FamilyNames)` (F1, IAM-1). */
@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val role: String,
    val givenNames: String,
    val familyNames: String,
)

/** `UserResource(UserId, Email, Role, CreatedAt, GivenNames, FamilyNames, FullName, PreferredLanguage)`. */
@Serializable
data class UserDto(
    val userId: Long,
    val email: String,
    val role: String,
    val createdAt: String? = null,
    val givenNames: String? = null,
    val familyNames: String? = null,
    val fullName: String? = null,
    val preferredLanguage: String? = null,
)
