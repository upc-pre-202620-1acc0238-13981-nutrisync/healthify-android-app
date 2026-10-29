package pe.edu.upc.healthify.features.iam.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `SignInResource(Email, Password)`, without `role` (single sign-in). */
@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String,
)

/**
 * `SignInResponseResource(UserId, Email, Role, SessionId, Token, GivenNames, FamilyNames, PreferredLanguage,
 * RefreshToken, ExpiresAt)`.
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

/** Body of `POST /authentication/token-refreshes`. */
@Serializable
data class TokenRefreshRequestDto(
    val refreshToken: String,
)

/** `200` from `POST /authentication/token-refreshes`: `{ token, refreshToken, expiresAt }`. */
@Serializable
data class TokenRefreshResponseDto(
    val token: String,
    val refreshToken: String,
    val expiresAt: String? = null,
)

/** `PasswordResetRequestResource(Email)`. */
@Serializable
data class PasswordResetRequestDto(
    val email: String,
)

/** `PasswordResetVerificationResource(Email, Code)`. */
@Serializable
data class PasswordResetVerificationRequestDto(
    val email: String,
    val code: String,
)

/** `200` from `POST /authentication/password-reset-verifications`: `{ resetToken, expiresAt }`. */
@Serializable
data class PasswordResetVerificationResponseDto(
    val resetToken: String,
    val expiresAt: String? = null,
)

/** `PasswordResetResource(ResetToken, NewPassword)`. */
@Serializable
data class PasswordResetDto(
    val resetToken: String,
    val newPassword: String,
)

/** `SignUpResource(Email, Password, Role, GivenNames, FamilyNames)`. */
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
