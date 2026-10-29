package pe.edu.upc.healthify.features.iam.infrastructure.mapper

import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.PasswordReset
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.PasswordResetVerificationResponseDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignUpRequestDto

private const val SHELL_PATIENT = "PatientShell"
private const val SHELL_PRACTITIONER = "PractitionerShell"

fun NewAccount.toDto(): SignUpRequestDto = SignUpRequestDto(
    email = email.value,
    password = password.value,
    role = role.toCode(),
    givenNames = name.givenNames,
    familyNames = name.familyNames,
)

fun Credentials.toDto(): SignInRequestDto = SignInRequestDto(email = email.value, password = password)

fun PasswordReset.toDto(): PasswordResetDto =
    PasswordResetDto(resetToken = token.value, newPassword = newPassword.value)

/** @throws IllegalArgumentException if the backend returned an empty token (broken contract). */
fun PasswordResetVerificationResponseDto.toDomain(): PasswordResetToken = PasswordResetToken(resetToken)

/** `null` if the session already ended or has no shell; an unknown value is also read as "no shell". */
fun NavigationShellDto.toDomain(): NavigationShell? = when {
    !isActive -> null
    navigationShell.equals(SHELL_PATIENT, ignoreCase = true) -> NavigationShell.PATIENT
    navigationShell.equals(SHELL_PRACTITIONER, ignoreCase = true) -> NavigationShell.PRACTITIONER
    else -> null
}
