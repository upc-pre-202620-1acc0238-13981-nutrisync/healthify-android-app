package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** PR20 · Ajustes del nutricionista (+ PR20.I hoja de idioma). */
data class PractitionerSettingsUiState(
    val fullName: String = "",
    val email: String = "",
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
)
