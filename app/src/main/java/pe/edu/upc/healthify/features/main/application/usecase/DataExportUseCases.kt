package pe.edu.upc.healthify.features.main.application.usecase

import pe.edu.upc.healthify.features.main.domain.repository.DataExportRepository
import pe.edu.upc.healthify.features.main.domain.valueobject.DataExportFileName
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** "Download a copy of my data": the name suggested to the file picker (today's date). */
class GetDataExportFileNameUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(): DataExportFileName = DataExportFileName.forDate(LocalDate.now(clock))
}

/** "Download a copy of my data": downloads the ZIP and saves it to the chosen document. */
class DownloadDataExportUseCase @Inject constructor(
    private val repository: DataExportRepository,
) {
    suspend operator fun invoke(patientUserId: Long, destinationUri: String): Result<Unit> =
        repository.download(patientUserId, destinationUri)
}
