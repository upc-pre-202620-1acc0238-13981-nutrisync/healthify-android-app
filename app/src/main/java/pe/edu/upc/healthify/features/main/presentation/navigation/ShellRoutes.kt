package pe.edu.upc.healthify.features.main.presentation.navigation

import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.core.designsystem.component.PatientTab
import pe.edu.upc.healthify.core.designsystem.component.PractitionerTab

/** Shell del paciente (NavigationBar de 5 destinos). */
@Serializable
data object PatientShellRoute

/** Shell del nutricionista (NavigationBar de 4 destinos). */
@Serializable
data object PractitionerShellRoute

// Destinos de cada pestaña del shell (el NavHost propio de cada shell).

/** PT3 · Inicio. */
@Serializable
data object PatientHomeRoute

@Serializable
data object PatientDiaryRoute

@Serializable
data object PatientProgressRoute

@Serializable
data object PatientRecordRoute

@Serializable
data object PatientSettingsRoute

/** PT21.1 · Cerrar sesión (pantalla completa del grafo raíz sobre el shell). */
@Serializable
data object SignOutConfirmationRoute

/** PR20.1 · Cerrar sesión del nutricionista (pantalla completa del grafo raíz sobre el shell). */
@Serializable
data object PractitionerSignOutConfirmationRoute

/**
 * Ficha de un paciente del nutricionista (PAC-0 a PAC-4), pantalla completa del grafo raíz. Los ViewModels de las
 * pestañas leen `patientId`, `careLinkId` y `patientName` del `SavedStateHandle` por estos nombres de propiedad
 * (`PatientArgs` de NutritionalCare y `PatientFollowUpViewModel.ARG_PATIENT_ID`).
 */
@Serializable
data class PractitionerPatientRoute(val patientId: Long, val careLinkId: Long, val patientName: String)

/** PR1 · Pacientes. */
@Serializable
data object PractitionerPatientsRoute

@Serializable
data object PractitionerInboxRoute

@Serializable
data object PractitionerAgendaRoute

@Serializable
data object PractitionerSettingsRoute

internal fun PatientTab.route(): Any = when (this) {
    PatientTab.Home -> PatientHomeRoute
    PatientTab.Diary -> PatientDiaryRoute
    PatientTab.Progress -> PatientProgressRoute
    PatientTab.Record -> PatientRecordRoute
    PatientTab.Settings -> PatientSettingsRoute
}

internal fun PractitionerTab.route(): Any = when (this) {
    PractitionerTab.Patients -> PractitionerPatientsRoute
    PractitionerTab.Inbox -> PractitionerInboxRoute
    PractitionerTab.Agenda -> PractitionerAgendaRoute
    PractitionerTab.Settings -> PractitionerSettingsRoute
}
