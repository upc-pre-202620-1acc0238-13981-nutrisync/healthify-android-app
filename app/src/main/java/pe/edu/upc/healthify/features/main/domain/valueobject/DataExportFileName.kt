package pe.edu.upc.healthify.features.main.domain.valueobject

import java.time.LocalDate

/** Nombre sugerido de la copia de datos: `healthify-mis-datos-<yyyy-MM-dd>.zip`, el mismo que propone el backend. */
@JvmInline
value class DataExportFileName(val value: String) {
    companion object {
        const val MIME_TYPE = "application/zip"

        fun forDate(date: LocalDate): DataExportFileName = DataExportFileName("healthify-mis-datos-$date.zip")
    }
}
