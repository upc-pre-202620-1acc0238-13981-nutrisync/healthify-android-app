package pe.edu.upc.healthify.features.main.domain.valueobject

import java.time.LocalDate

/** Suggested name for the data copy: `healthify-mis-datos-<yyyy-MM-dd>.zip`, the same one the backend suggests. */
@JvmInline
value class DataExportFileName(val value: String) {
    companion object {
        const val MIME_TYPE = "application/zip"

        fun forDate(date: LocalDate): DataExportFileName = DataExportFileName("healthify-mis-datos-$date.zip")
    }
}
