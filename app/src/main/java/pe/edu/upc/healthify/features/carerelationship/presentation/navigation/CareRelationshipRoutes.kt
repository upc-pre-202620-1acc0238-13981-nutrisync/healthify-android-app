package pe.edu.upc.healthify.features.carerelationship.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * PT1 · Escanear invitación.
 *
 * @param replaceActiveLink `true` solo si se llega desde la confirmación de PT21.V (CR-1): el canje revoca el vínculo
 *   actual. Desde S5 o PT24 (paciente sin vínculo) va en `false`.
 */
@Serializable
data class ScanInvitationRoute(val replaceActiveLink: Boolean = false) {
    companion object {
        /** Clave del argumento en el `SavedStateHandle` del ViewModel (nombre de la propiedad). */
        const val ARG_REPLACE_ACTIVE_LINK = "replaceActiveLink"
    }
}

/**
 * PT2 · Consentimiento y alcance del vínculo [careLinkId].
 *
 * @param fromPendingConsent `true` si se abrió desde PT2.1 (atrás vuelve a ella); `false` si viene de canjear en PT1,
 *   que ya no tiene sentido mostrar: atrás lleva a PT2.1, igual que «Ahora no».
 */
@Serializable
data class ConsentRoute(val careLinkId: Long, val fromPendingConsent: Boolean = false) {
    companion object {
        const val ARG_CARE_LINK_ID = "careLinkId"
    }
}

/** PT2.1 · Vínculo pendiente de consentimiento. */
@Serializable
data object PendingConsentRoute

/** PT21.V · «¿Cambiar de nutricionista?» (diálogo sobre Ajustes). */
@Serializable
data object SwitchPractitionerRoute

/** PT23 · Retirar consentimiento (+ PT23.M confirmación final). */
@Serializable
data object WithdrawConsentRoute

/** PT24 · Consentimiento retirado. */
@Serializable
data object ConsentWithdrawnRoute

/** PT21.IA · Ajustes — Funciones con IA. */
@Serializable
data object AiFeaturesRoute

/** PR2 · Generar invitación (pantalla completa sobre el shell del nutricionista). */
@Serializable
data object InvitePatientRoute

/** PR18 · Alta clínica (desde PAC-1 «Dar de alta»). */
@Serializable
data class DischargePatientRoute(val careLinkId: Long, val patientName: String) {
    companion object {
        const val ARG_CARE_LINK_ID = "careLinkId"
        const val ARG_PATIENT_NAME = "patientName"
    }
}
