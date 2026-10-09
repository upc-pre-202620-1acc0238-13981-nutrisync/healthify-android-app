package pe.edu.upc.healthify.features.carerelationship.presentation.state

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R

/** Adónde lleva el botón del aviso de un error de PT2. */
enum class ConsentErrorAction { Dismiss, GoHome, ScanInvitation }

/** Validaciones de PT2 (frame «Notas») por `extensions.code` de F6. */
enum class ConsentError(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    @param:StringRes val actionRes: Int,
    val action: ConsentErrorAction,
) {
    /** `ConsentScopeRequired` (400). No debería ocurrir: el alcance es fijo (`ConsentScope.CURRENT`). */
    ScopeRequired(
        R.string.consent_error_scope_title,
        R.string.consent_error_scope,
        R.string.common_understood,
        ConsentErrorAction.Dismiss,
    ),

    /** `DischargedLinkCannotBeReactivated` (409). */
    LinkDischarged(
        R.string.consent_error_closed_title,
        R.string.consent_error_discharged,
        R.string.consent_error_scan_action,
        ConsentErrorAction.ScanInvitation,
    ),

    /** `CareLinkAlreadyRevoked` (409). */
    LinkRevoked(
        R.string.consent_error_closed_title,
        R.string.consent_error_revoked,
        R.string.consent_error_scan_action,
        ConsentErrorAction.ScanInvitation,
    ),

    /** `ConsentAlreadyGranted` (409): el vínculo ya está activo. */
    AlreadyGranted(
        R.string.consent_error_already_title,
        R.string.consent_error_already_granted,
        R.string.consent_error_home_action,
        ConsentErrorAction.GoHome,
    ),
}

/**
 * PT2 · Consentimiento y alcance.
 *
 * @param aiProcessingGranted interruptor «Usar funciones con IA» (CR-2). DECISIÓN PT2: empieza **apagado** aunque el
 *   frame lo dibuje encendido; la IA es un consentimiento aparte y debe ser un acto explícito del paciente (Ley 29733;
 *   el backend también asume `false` si falta).
 * @param showServerError «Vínculo no existe → error servidor» y fallas de red/5xx: diálogo Reintentar/Cerrar.
 */
data class ConsentUiState(
    val aiProcessingGranted: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val error: ConsentError? = null,
    val showServerError: Boolean = false,
)

sealed interface ConsentEvent {
    data object NavigateToHome : ConsentEvent
    data object NavigateToPendingConsent : ConsentEvent
    data object NavigateToScanInvitation : ConsentEvent
}
