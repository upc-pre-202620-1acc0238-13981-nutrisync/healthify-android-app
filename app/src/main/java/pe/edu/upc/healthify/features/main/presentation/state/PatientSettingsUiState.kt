package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** PT21 · Ajustes del paciente (+ PT21.I hoja de idioma y la descarga de la copia de datos). */
data class PatientSettingsUiState(
    val fullName: String = "",
    val email: String = "",
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    /** «Funciones con IA · Activadas/Desactivadas»; `null` mientras no se sabe. */
    val aiFeaturesActive: Boolean? = null,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
    /** «Descargar una copia de mis datos». */
    val dataExport: DataExportStatus = DataExportStatus.Idle,
)

/** Estado de la descarga de la copia de datos (ZIP de CSV). */
sealed interface DataExportStatus {
    data object Idle : DataExportStatus

    /** Descargando y guardando en el archivo elegido: el ítem se deshabilita. */
    data object InProgress : DataExportStatus

    /** «Listo, guardamos tu copia». */
    data object Saved : DataExportStatus

    /** No se pudo; [offline] si fue por falta de conexión. */
    data class Failed(val offline: Boolean) : DataExportStatus
}
