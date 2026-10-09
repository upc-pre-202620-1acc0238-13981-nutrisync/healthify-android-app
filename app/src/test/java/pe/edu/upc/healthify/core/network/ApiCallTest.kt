package pe.edu.upc.healthify.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiCallTest {

    @Test
    fun `success is wrapped`() = runTest {
        val result = apiCall { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `http error becomes a domain error`() = runTest {
        val body = """{"status":409,"title":"Conflicto"}""".toResponseBody("application/problem+json".toMediaType())

        val result = apiCall<Int> { throw HttpException(Response.error<Int>(409, body)) }

        assertEquals(DomainError.Conflict(null), result.domainErrorOrNull())
    }

    @Test
    fun `io error becomes network`() = runTest {
        val result = apiCall<Int> { throw IOException("offline") }

        assertEquals(DomainError.Network, result.domainErrorOrNull())
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        val thrown = runCatching { apiCall<Int> { throw CancellationException("cancelled") } }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }
}
