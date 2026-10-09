package pe.edu.upc.healthify.features.iam.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `NavigationShellResource(SessionId, RoleClaim, NavigationShell, IsActive)`. `navigationShell` es `PatientShell`,
 * `PractitionerShell` o `null` mientras no se eligió ninguno.
 */
@Serializable
data class NavigationShellDto(
    val sessionId: Long,
    val roleClaim: String? = null,
    val navigationShell: String? = null,
    val isActive: Boolean = false,
)

/** `ChangePreferredLanguageResource(Language)` (IAM-3): `es` o `en`. */
@Serializable
data class ChangePreferredLanguageRequestDto(
    val language: String,
)
