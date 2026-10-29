package pe.edu.upc.healthify.features.main.infrastructure.repository

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.main.domain.repository.DataExportRepository
import pe.edu.upc.healthify.features.main.domain.valueobject.DataExportFileName
import pe.edu.upc.healthify.features.main.infrastructure.file.DocumentWriter
import pe.edu.upc.healthify.features.main.infrastructure.remote.DataExportService
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

/** Data export: the ZIP is requested from the backend and written as is to the chosen document. */
class DataExportRepositoryImplTest {

    private val server = MockWebServer()
    private val writer = FakeDocumentWriter()
    private lateinit var repository: DataExportRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(OkHttpClient())
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DataExportService::class.java)
        repository = DataExportRepositoryImpl(service, writer)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the zip is downloaded for the patient and written byte for byte in the chosen document`() = runTest {
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 0, -1)
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .setHeader("Content-Type", "application/zip")
                .body(Buffer().write(zip))
                .build(),
        )

        val result = repository.download(12, DESTINATION)

        assertTrue(result.isSuccess)
        assertEquals("/api/v1/patients/12/data-export", server.takeRequest().url.encodedPath)
        assertEquals(DESTINATION, writer.uri)
        assertArrayEquals(zip, writer.written)
    }

    @Test
    fun `another patient's copy is refused and nothing is written`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(403)
                .setHeader("Content-Type", "application/problem+json")
                .body("""{"status":403,"title":"Forbidden","code":"AccessNotAllowed"}""")
                .build(),
        )

        val result = repository.download(99, DESTINATION)

        assertEquals(DomainError.Forbidden("AccessNotAllowed"), result.domainErrorOrNull())
        assertEquals(null, writer.uri)
    }

    @Test
    fun `a document that cannot be written is reported as not saved`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("zip").build())
        writer.failure = IOException("disk full")

        val result = repository.download(12, DESTINATION)

        assertEquals(
            DomainError.Unexpected(DataExportRepository.CODE_NOT_SAVED),
            result.domainErrorOrNull(),
        )
    }

    @Test
    fun `the suggested name carries the date`() {
        assertEquals(
            "healthify-mis-datos-2026-10-21.zip",
            DataExportFileName.forDate(LocalDate.of(2026, 10, 21)).value,
        )
    }

    private class FakeDocumentWriter : DocumentWriter {
        var uri: String? = null
        var written: ByteArray = byteArrayOf()
        var failure: IOException? = null

        override suspend fun write(uri: String, content: InputStream) {
            failure?.let { throw it }
            this.uri = uri
            written = content.readBytes()
        }
    }

    private companion object {
        const val DESTINATION = "content://com.android.providers.downloads.documents/document/42"
    }
}
