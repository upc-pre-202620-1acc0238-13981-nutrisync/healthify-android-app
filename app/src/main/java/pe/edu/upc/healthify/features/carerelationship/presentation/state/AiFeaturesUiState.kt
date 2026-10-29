package pe.edu.upc.healthify.features.carerelationship.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences

/** AI features. */
data class AiFeaturesUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val preferences: AiPreferences = AiPreferences.NoneGranted,
    /** The feature being saved (its switch cannot be toggled meanwhile). */
    val savingFeature: AiFeature? = null,
    /** "Turn on AI features?" dialog: one was turned on without consent (or "Turn on" was tapped). */
    val consentOffer: ConsentOffer? = null,
    val isGrantingConsent: Boolean = false,
) {
    val consentGranted: Boolean get() = preferences.consentGranted

    /** On for the patient: current consent **and** its preference. */
    fun isOn(feature: AiFeature): Boolean = preferences.consentGranted && preferences.isEnabled(feature)

    /** Changing anything needs a connection (it is saved on the backend). */
    val canChange: Boolean get() = !isLoading && !isOffline && savingFeature == null && !isGrantingConsent
}

/** @param feature the feature the patient tried to turn on, turned on once accepted; `null` = consent only. */
data class ConsentOffer(val feature: AiFeature?)
