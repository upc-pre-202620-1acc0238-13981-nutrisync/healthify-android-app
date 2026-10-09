package pe.edu.upc.healthify.features.carerelationship.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences

/** PT21.IA · Funciones con IA (IA-1, CR-2). */
data class AiFeaturesUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val preferences: AiPreferences = AiPreferences.NoneGranted,
    /** La función que se está guardando (su interruptor no se puede tocar mientras tanto). */
    val savingFeature: AiFeature? = null,
    /** Diálogo «¿Activar las funciones con IA?»: se encendió una sin consentimiento (o se tocó «Activar»). */
    val consentOffer: ConsentOffer? = null,
    val isGrantingConsent: Boolean = false,
) {
    val consentGranted: Boolean get() = preferences.consentGranted

    /** Encendida para el paciente: consentimiento vigente **y** su preferencia. */
    fun isOn(feature: AiFeature): Boolean = preferences.consentGranted && preferences.isEnabled(feature)

    /** Cambiar algo necesita conexión (se guarda en el backend). */
    val canChange: Boolean get() = !isLoading && !isOffline && savingFeature == null && !isGrantingConsent
}

/** @param feature la función que se quería encender, que se enciende al aceptar; `null` = solo el consentimiento. */
data class ConsentOffer(val feature: AiFeature?)
