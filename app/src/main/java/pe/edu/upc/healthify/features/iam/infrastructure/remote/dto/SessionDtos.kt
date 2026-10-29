package pe.edu.upc.healthify.features.iam.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `NavigationShellResource(SessionId, RoleClaim, NavigationShell, IsActive)`. `navigationShell` is `PatientShell`,
 * `PractitionerShell` or `null` while none has been chosen.
 */
@Serializable
data class NavigationShellDto(
    val sessionId: Long,
    val roleClaim: String? = null,
    val navigationShell: String? = null,
    val isActive: Boolean = false,
)

/** `ChangePreferredLanguageResource(Language)`: `es` or `en`. */
@Serializable
data class ChangePreferredLanguageRequestDto(
    val language: String,
)
