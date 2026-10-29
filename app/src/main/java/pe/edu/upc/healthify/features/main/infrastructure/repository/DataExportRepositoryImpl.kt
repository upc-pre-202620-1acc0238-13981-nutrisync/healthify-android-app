package pe.edu.upc.healthify.features.main.infrastructure.repository

import kotlinx.coroutines.CancellationException
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.main.domain.repository.DataExportRepository
import pe.edu.upc.healthify.features.main.infrastructure.file.DocumentWriter
import pe.edu.upc.healthify.features.main.infrastructure.remote.DataExportService
import java.io.IOException
import javax.inject.Inject

class DataExportRepositoryImpl @Inject constructor(
    private val service: DataExportService,
    private val writer: DocumentWriter,
) : DataExportRepository {

    override suspend fun download(patientUserId: Long, destinationUri: String): Result<Unit> {
        val body = apiCall { service.download(patientUserId) }.getOrElse { return Result.failure(it) }
        // The body is read while it is written (@Streaming): a drop halfway through the download also ends up here.
        return try {
            body.use { writer.write(destinationUri, it.byteStream()) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            domainFailure(DomainError.Unexpected(DataExportRepository.CODE_NOT_SAVED))
        } catch (_: SecurityException) {
            domainFailure(DomainError.Unexpected(DataExportRepository.CODE_NOT_SAVED))
        }
    }
}
