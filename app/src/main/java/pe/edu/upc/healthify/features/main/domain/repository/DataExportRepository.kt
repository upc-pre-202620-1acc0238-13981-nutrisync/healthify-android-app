package pe.edu.upc.healthify.features.main.domain.repository

/**
 * Copia de los datos del paciente: un ZIP de CSV (`GET /patients/{pid}/data-export`). No se guarda nada en la app: se
 * escribe directo en el documento que eligió la persona.
 */
interface DataExportRepository {

    /**
     * Descarga la copia y la escribe en [destinationUri] (un `content://` de `CreateDocument`). Sin red →
     * `DomainError.Network`; `403 AccessNotAllowed` si no es el propio paciente; si no se pudo escribir el documento →
     * `DomainError.Unexpected(CODE_NOT_SAVED)`.
     */
    suspend fun download(patientUserId: Long, destinationUri: String): Result<Unit>

    companion object {
        const val CODE_NOT_SAVED = "DATA_EXPORT_NOT_SAVED"
    }
}
