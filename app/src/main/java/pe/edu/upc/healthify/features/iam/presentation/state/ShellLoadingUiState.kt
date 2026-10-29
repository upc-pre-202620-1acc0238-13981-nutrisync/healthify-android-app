package pe.edu.upc.healthify.features.iam.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell

/** Where the person goes after loading. */
enum class ShellDestination {
    /** Patient without a care link. */
    PatientScanInvitation,

    /** Patient with a care link awaiting consent. */
    PatientPendingConsent,

    /** Patient with an active care link (patient shell). */
    PatientHome,

    /** Practitioner (practitioner shell). */
    PractitionerHome,
}

/**
 * Loading rule: the shell decides; inside the patient shell, the care link status picks the screen
 * (signing up grants no access: without an active care link no clinical data is shown).
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

/** Loading (Normal / Error). */
data class ShellLoadingUiState(
    val isError: Boolean = false,
    val isRetrying: Boolean = false,
)

sealed interface ShellLoadingEvent {
    data class Navigate(val destination: ShellDestination) : ShellLoadingEvent

    /** No stored user (should not happen right after sign-up or sign-in): goes back to the start. */
    data object NavigateToWelcome : ShellLoadingEvent
}
