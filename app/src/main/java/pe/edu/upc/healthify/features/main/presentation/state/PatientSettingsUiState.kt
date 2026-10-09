package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** PT21 · Ajustes del paciente (+ PT21.I hoja de idioma y el diálogo «Próximamente» de la copia de datos). */
data class PatientSettingsUiState(
    val fullName: String = "",
    val email: String = "",
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    /** «Funciones con IA · Activadas/Desactivadas»; `null` mientras no se sabe. */
    val aiFeaturesActive: Boolean? = null,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
    val showDataExportDialog: Boolean = false,
)
