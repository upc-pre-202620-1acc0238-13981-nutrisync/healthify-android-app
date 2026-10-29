package pe.edu.upc.healthify.features.main.presentation.navigation

import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.core.designsystem.component.PractitionerTab

/** Patient shell (4-destination NavigationBar and the camera button). */
@Serializable
data object PatientShellRoute

/** Practitioner shell (3-destination NavigationBar). */
@Serializable
data object PractitionerShellRoute

// Destinations of each shell tab (each shell's own NavHost).

/** Home. */
@Serializable
data object PatientHomeRoute

@Serializable
data object PatientDiaryRoute

@Serializable
data object PatientProgressRoute

@Serializable
data object PatientRecordRoute

/** Patient settings: opened from the main menu (full screen in the root graph, over the shell). */
@Serializable
data object PatientSettingsRoute

/** Practitioner settings: opened from the main menu (full screen in the root graph, over the shell). */
@Serializable
data object PractitionerSettingsRoute

/** Sign out (full screen in the root graph, over the shell). */
@Serializable
data object SignOutConfirmationRoute

/** Practitioner sign out (full screen in the root graph, over the shell). */
@Serializable
data object PractitionerSignOutConfirmationRoute

/**
 * A practitioner's patient record, full screen in the root graph. The tab ViewModels read `patientId`,
 * `careLinkId` and `patientName` from the `SavedStateHandle` by these property names
 * (NutritionalCare's `PatientArgs` and `PatientFollowUpViewModel.ARG_PATIENT_ID`).
 */
@Serializable
data class PractitionerPatientRoute(val patientId: Long, val careLinkId: Long, val patientName: String)

/** Patients. */
@Serializable
data object PractitionerPatientsRoute

@Serializable
data object PractitionerInboxRoute

@Serializable
data object PractitionerAgendaRoute

internal fun PatientTab.route(): Any = when (this) {
    PatientTab.Home -> PatientHomeRoute
    PatientTab.Diary -> PatientDiaryRoute
    PatientTab.Progress -> PatientProgressRoute
    PatientTab.Record -> PatientRecordRoute
}

internal fun PractitionerTab.route(): Any = when (this) {
    PractitionerTab.Patients -> PractitionerPatientsRoute
    PractitionerTab.Inbox -> PractitionerInboxRoute
    PractitionerTab.Agenda -> PractitionerAgendaRoute
}
