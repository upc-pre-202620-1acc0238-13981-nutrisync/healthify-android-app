package pe.edu.upc.healthify.features.iam.presentation.state

/**
 * Session expired. The patient and practitioner frames are the same; the visual changes with the connection: dark
 * (Normal) or light with a banner and the "sign in again once you are online" text (Offline).
 */
data class SessionExpiredUiState(
    val isOffline: Boolean = false,
)
