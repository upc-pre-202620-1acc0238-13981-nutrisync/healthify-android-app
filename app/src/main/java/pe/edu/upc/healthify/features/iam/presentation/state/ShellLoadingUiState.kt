package pe.edu.upc.healthify.features.iam.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell

/** Adónde entra la persona después de S5. */
enum class ShellDestination {
    /** PT1 · paciente sin vínculo. */
    PatientScanInvitation,

    /** PT2.1 · paciente con vínculo pendiente de consentimiento. */
    PatientPendingConsent,

    /** PT3 · paciente con vínculo activo (shell del paciente). */
    PatientHome,

    /** PR1 · nutricionista (shell del nutricionista). */
    PractitionerHome,
}

/**
 * Regla de S5: el shell manda; dentro del shell del paciente, el estado del vínculo elige la pantalla
 * (registrarse no da acceso: sin vínculo activo no se muestran datos clínicos).
 */
fun selectShellDestination(shell: NavigationShell, linkStatus: PatientLinkStatus?): ShellDestination =
    when (shell) {
        NavigationShell.PRACTITIONER -> ShellDestination.PractitionerHome
        NavigationShell.PATIENT -> when (linkStatus) {
            PatientLinkStatus.ACTIVE -> ShellDestination.PatientHome
            PatientLinkStatus.PENDING_CONSENT -> ShellDestination.PatientPendingConsent
            PatientLinkStatus.NO_LINK, null -> ShellDestination.PatientScanInvitation
        }
    }

/** S5 · Cargando (Normal / Error). */
data class ShellLoadingUiState(
    val isError: Boolean = false,
    val isRetrying: Boolean = false,
)

sealed interface ShellLoadingEvent {
    data class Navigate(val destination: ShellDestination) : ShellLoadingEvent

    /** No hay usuario guardado (no debería pasar al venir de S1/S3/S4): vuelve a S2. */
    data object NavigateToWelcome : ShellLoadingEvent
}
