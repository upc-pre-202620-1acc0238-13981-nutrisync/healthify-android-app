package pe.edu.upc.healthify.features.carerelationship.presentation.state

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R

/** Validaciones de PT1 (frame «Notas»), por `extensions.code` del canje (F5). Ningún mensaje culpa al paciente. */
enum class ScanInvitationError(@param:StringRes val messageRes: Int) {
    /** `InvitationNotValid` (400) o un QR que no tiene forma de invitación. */
    InvalidFormat(R.string.scan_invitation_error_invalid),

    /** `InvitationNotFound` (404). */
    NotFound(R.string.scan_invitation_error_not_found),

    /** `InvitationAlreadyRedeemed` (409). */
    AlreadyUsed(R.string.scan_invitation_error_used),

    /** `InvitationExpired` (422). */
    Expired(R.string.scan_invitation_error_expired),

    /** `PatientCannotSelfLink` (403). */
    SelfLink(R.string.scan_invitation_error_self_link),

    /** `PatientAlreadyHasActiveLink` (409): se escaneó sin pasar por PT21.V. */
    AlreadyLinked(R.string.scan_invitation_error_already_linked),

    /** `AlreadyLinkedToThisPractitioner` (409, CR-1): desde PT21.V, el código es del mismo nutricionista. */
    SamePractitioner(R.string.scan_invitation_error_same_practitioner),

    /** Sin conexión al enviar. */
    Network(R.string.error_network),

    /** Cualquier otra respuesta (5xx, 403 sin código…). */
    Unexpected(R.string.ds_error_state_body),
}

/** Permiso de cámara: aún sin resolver, concedido o denegado (PT1.M). */
enum class CameraPermissionState { Unknown, Granted, Denied }

/**
 * PT1 · Escanear invitación (+ PT1.M permiso de cámara).
 *
 * @param isRedeeming «Buscando el código…»: el QR se está canjeando y la lectura se pausa.
 * @param error mensaje de la última lectura rechazada; se quita al leer un código distinto.
 * @param showNoCameraDialog DECISIÓN PT1: «No tengo cámara disponible» no tiene frame ni otro camino (PR2 solo
 *   muestra el QR); abre un aviso informativo.
 * @param showSignOutDialog DECISIÓN PT1: cuando PT1 es la primera pantalla (S5, PT24), «atrás» ofrece cerrar sesión
 *   con el modal de PT21.1; si no, quien no tiene invitación no podría salir de su cuenta.
 */
data class ScanInvitationUiState(
    val replaceActiveLink: Boolean = false,
    val cameraPermission: CameraPermissionState = CameraPermissionState.Unknown,
    val isRedeeming: Boolean = false,
    val error: ScanInvitationError? = null,
    val isOffline: Boolean = false,
    val showNoCameraDialog: Boolean = false,
    val showSignOutDialog: Boolean = false,
    val isSigningOut: Boolean = false,
) {
    /** La cámara analiza solo con permiso, con red y sin un canje en curso. */
    val isScanning: Boolean
        get() = cameraPermission == CameraPermissionState.Granted && !isRedeeming && !isOffline
}

sealed interface ScanInvitationEvent {
    data class NavigateToConsent(val careLinkId: Long) : ScanInvitationEvent
    data object SignedOut : ScanInvitationEvent
}
