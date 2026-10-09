package pe.edu.upc.healthify.core.network

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError

class ProblemDetailsMapperTest {

    private fun problem(status: Int, code: String) =
        """{"type":"about:blank","title":"Título localizado","status":$status,"detail":"Detalle","code":"$code"}"""

    @Test
    fun `reads the top level code that X-3 sends`() {
        assertEquals(DomainError.Unauthorized("InvalidCredentials"), ProblemDetailsMapper.map(401, problem(401, "InvalidCredentials")))
        assertEquals(DomainError.Unauthorized("AccountLocked"), ProblemDetailsMapper.map(401, problem(401, "AccountLocked")))
        assertEquals(DomainError.Forbidden("AiConsentRequired"), ProblemDetailsMapper.map(403, problem(403, "AiConsentRequired")))
        assertEquals(DomainError.NotFound("CareLinkNotFound"), ProblemDetailsMapper.map(404, problem(404, "CareLinkNotFound")))
        assertEquals(DomainError.Conflict("EmailAlreadyTaken"), ProblemDetailsMapper.map(409, problem(409, "EmailAlreadyTaken")))
        assertEquals(DomainError.Validation("WeakPassword"), ProblemDetailsMapper.map(400, problem(400, "WeakPassword")))
        assertEquals(DomainError.Validation("PhotoTooLarge"), ProblemDetailsMapper.map(413, problem(413, "PhotoTooLarge")))
        assertEquals(DomainError.Unexpected("AiQuotaExceeded"), ProblemDetailsMapper.map(429, problem(429, "AiQuotaExceeded")))
        assertEquals(DomainError.Unexpected("InternalError"), ProblemDetailsMapper.map(500, problem(500, "InternalError")))
        assertEquals(DomainError.Unexpected("AiProviderUnavailable"), ProblemDetailsMapper.map(503, problem(503, "AiProviderUnavailable")))
    }

    @Test
    fun `distinguishes two 401s only by code`() {
        val invalid = ProblemDetailsMapper.map(401, problem(401, "InvalidCredentials"))
        val locked = ProblemDetailsMapper.map(401, problem(401, "AccountLocked"))

        assertEquals("InvalidCredentials", (invalid as DomainError.Unauthorized).code)
        assertEquals("AccountLocked", (locked as DomainError.Unauthorized).code)
    }

    @Test
    fun `validation problem keeps its code and ignores the errors map`() {
        val body = """{"status":400,"title":"x","errors":{"Email":["required"]},"code":"ValidationFailed"}"""

        assertEquals(DomainError.Validation("ValidationFailed"), ProblemDetailsMapper.map(400, body))
    }

    @Test
    fun `extra extensions do not interfere`() {
        val body = """{"status":409,"code":"ConsultationAlreadyOpen","consultationId":42}"""

        assertEquals(DomainError.Conflict("ConsultationAlreadyOpen"), ProblemDetailsMapper.map(409, body))
    }

    @Test
    fun `reads code nested in extensions as a fallback`() {
        val body = """{"status":401,"extensions":{"code":"AccountLocked"}}"""

        assertEquals(DomainError.Unauthorized("AccountLocked"), ProblemDetailsMapper.map(401, body))
    }

    @Test
    fun `empty framework responses decide by status`() {
        assertEquals(DomainError.Forbidden(null), ProblemDetailsMapper.map(403, ""))
        assertEquals(DomainError.NotFound(null), ProblemDetailsMapper.map(404, null))
        assertEquals(DomainError.Unexpected("HTTP_405"), ProblemDetailsMapper.map(405, ""))
        assertEquals(DomainError.Validation("HTTP_415"), ProblemDetailsMapper.map(415, ""))
    }

    @Test
    fun `malformed body or non string code does not crash`() {
        assertEquals(DomainError.Validation("HTTP_400"), ProblemDetailsMapper.map(400, "<html>"))
        assertEquals(DomainError.Validation("HTTP_422"), ProblemDetailsMapper.map(422, """{"code":12}"""))
        assertEquals(DomainError.Conflict(null), ProblemDetailsMapper.map(409, """{"code":"  "}"""))
        assertEquals(DomainError.Unexpected("HTTP_500"), ProblemDetailsMapper.map(500, "[]"))
    }
}
