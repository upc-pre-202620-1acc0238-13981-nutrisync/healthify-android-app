package pe.edu.upc.healthify.features.main.application.usecase

import pe.edu.upc.healthify.features.main.domain.repository.DataExportRepository
import pe.edu.upc.healthify.features.main.domain.valueobject.DataExportFileName
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** PT21 «Descargar una copia de mis datos»: el nombre que se le propone al selector de archivos (fecha de hoy). */
class GetDataExportFileNameUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(): DataExportFileName = DataExportFileName.forDate(LocalDate.now(clock))
}

/** PT21 «Descargar una copia de mis datos»: descarga el ZIP y lo guarda en el documento elegido. */
class DownloadDataExportUseCase @Inject constructor(
    private val repository: DataExportRepository,
) {
    suspend operator fun invoke(patientUserId: Long, destinationUri: String): Result<Unit> =
        repository.download(patientUserId, destinationUri)
}
