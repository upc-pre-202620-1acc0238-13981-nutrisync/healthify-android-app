package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** Practitioner settings (+ the language sheet). */
data class PractitionerSettingsUiState(
    val fullName: String = "",
    val email: String = "",
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
)
