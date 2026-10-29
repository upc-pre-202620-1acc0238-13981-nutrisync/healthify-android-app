package pe.edu.upc.healthify.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pe.edu.upc.healthify.core.common.error.DomainError

/**
 * Translates a backend error response (RFC 7807, `application/problem+json`) into a [DomainError].
 *
 * Every ProblemDetails carries `extensions.code` with the name of the error enum value (`InvalidCredentials`,
 * `AccountLocked`, `AuthenticationRequired`…). ASP.NET serializes `Extensions` flattened, so it arrives as a
 * top-level `code`; it is also accepted inside `extensions` just in case. The UI decides by that code, never by
 * `title`/`detail` (already localized). Table: `docs/backend/CODIGOS-DE-ERROR.md`.
 *
 * Empty framework responses (`Forbid()`, role 403, route 404, 405, 415) carry no `code`: the error
 * has no code, or `HTTP_<status>` in [DomainError.Validation]/[DomainError.Unexpected].
 */
object ProblemDetailsMapper {

    private const val CODE_KEY = "code"
    private const val EXTENSIONS_KEY = "extensions"

    private val json = Json { ignoreUnknownKeys = true }

    fun map(status: Int, body: String?): DomainError {
        val code = body?.let(::extractCode)
        return when (status) {
            401 -> DomainError.Unauthorized(code)
            403 -> DomainError.Forbidden(code)
            404 -> DomainError.NotFound(code)
            409 -> DomainError.Conflict(code)
            400, 413, 415, 422 -> DomainError.Validation(code ?: "HTTP_$status")
            else -> DomainError.Unexpected(code ?: "HTTP_$status")
        }
    }

    private fun extractCode(body: String): String? {
        if (body.isBlank()) return null
        val root = try {
            json.parseToJsonElement(body) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return null
        return root.codeField() ?: (root[EXTENSIONS_KEY] as? JsonObject)?.codeField()
    }

    private fun JsonObject.codeField(): String? =
        (this[CODE_KEY] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
