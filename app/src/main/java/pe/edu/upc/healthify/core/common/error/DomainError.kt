package pe.edu.upc.healthify.core.common.error

/**
 * Shared kernel de errores. Kotlin puro: lo usan domain, application y presentation de todos los contextos.
 *
 * `code` es el nombre del valor del enum de error del backend (`InvalidCredentials`, `ConsentAlreadyGranted`…)
 * cuando el backend lo envía; la UI elige el texto del mockup según ese código.
 */
sealed interface DomainError {
    data object Network : DomainError
    data class Unauthorized(val code: String? = null) : DomainError
    data class Forbidden(val code: String? = null) : DomainError
    data class NotFound(val code: String? = null) : DomainError
    data class Conflict(val code: String? = null) : DomainError
    data class Validation(val code: String) : DomainError

    /** 5xx, 429 o una respuesta que no se pudo interpretar. `code` lleva el status si no hay otro (`HTTP_503`). */
    data class Unexpected(val code: String? = null) : DomainError
}

/** Permite transportar un [DomainError] dentro de `Result.failure`. */
class DomainException(val error: DomainError) : Exception(error.toString())

/** El [DomainError] de un `Result` fallido; cualquier otra excepción se lee como [DomainError.Unexpected]. */
fun Result<*>.domainErrorOrNull(): DomainError? = exceptionOrNull()?.let { throwable ->
    (throwable as? DomainException)?.error ?: DomainError.Unexpected()
}

fun <T> domainFailure(error: DomainError): Result<T> = Result.failure(DomainException(error))
