package pe.edu.upc.healthify.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pe.edu.upc.healthify.core.common.error.DomainError

/**
 * Traduce una respuesta de error del backend (RFC 7807, `application/problem+json`) a [DomainError].
 *
 * X-3: todo ProblemDetails trae `extensions.code` con el nombre del valor del enum de error (`InvalidCredentials`,
 * `AccountLocked`, `AuthenticationRequired`…). ASP.NET serializa `Extensions` aplanado, así que llega como `code`
 * de primer nivel; se acepta también dentro de `extensions` por robustez. La UI decide por ese código, nunca por
 * `title`/`detail` (ya localizados). Tabla: `docs/backend/CODIGOS-DE-ERROR.md`.
 *
 * Las respuestas vacías del framework (`Forbid()`, 403 de rol, 404 de ruta, 405, 415) no traen `code`: el error
 * queda sin código, o con `HTTP_<status>` en [DomainError.Validation]/[DomainError.Unexpected].
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
