package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** Patient settings (+ the language sheet and the data copy download). */
data class PatientSettingsUiState(
    val fullName: String = "",
    val email: String = "",
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    /** "AI features · On/Off"; `null` while unknown. */
    val aiFeaturesActive: Boolean? = null,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
    /** "Download a copy of my data". */
    val dataExport: DataExportStatus = DataExportStatus.Idle,
)

/** State of the data copy download (ZIP of CSV files). */
sealed interface DataExportStatus {
    data object Idle : DataExportStatus

    /** Downloading and saving to the chosen file: the item is disabled. */
    data object InProgress : DataExportStatus

    /** "Done, we saved your copy". */
    data object Saved : DataExportStatus

    /** It failed; [offline] if it was for lack of connection. */
    data class Failed(val offline: Boolean) : DataExportStatus
}
