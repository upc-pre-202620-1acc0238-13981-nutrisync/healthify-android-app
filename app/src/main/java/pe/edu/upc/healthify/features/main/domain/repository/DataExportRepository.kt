package pe.edu.upc.healthify.features.main.domain.repository

/**
 * Copy of the patient's data: a ZIP of CSV files (`GET /patients/{pid}/data-export`). Nothing is stored in the app:
 * it is written straight to the document the person chose.
 */
interface DataExportRepository {

    /**
     * Downloads the copy and writes it to [destinationUri] (a `content://` from `CreateDocument`). Offline →
     * `DomainError.Network`; `403 AccessNotAllowed` if it is not the patient themselves; if the document could not be
     * written → `DomainError.Unexpected(CODE_NOT_SAVED)`.
     */
    suspend fun download(patientUserId: Long, destinationUri: String): Result<Unit>

    companion object {
        const val CODE_NOT_SAVED = "DATA_EXPORT_NOT_SAVED"
    }
}
