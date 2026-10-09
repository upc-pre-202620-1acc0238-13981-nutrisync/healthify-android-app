package pe.edu.upc.healthify.features.iam.infrastructure.mapper

import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
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

/** `null` si la sesión ya terminó o no tiene shell; un valor desconocido también se lee como «sin shell». */
fun NavigationShellDto.toDomain(): NavigationShell? = when {
    !isActive -> null
    navigationShell.equals(SHELL_PATIENT, ignoreCase = true) -> NavigationShell.PATIENT
    navigationShell.equals(SHELL_PRACTITIONER, ignoreCase = true) -> NavigationShell.PRACTITIONER
    else -> null
}
