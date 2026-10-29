package pe.edu.upc.healthify.core.common.error

/**
 * Shared error kernel. Pure Kotlin: domain, application and presentation of every context use it.
 *
 * `code` is the name of the backend error enum value (`InvalidCredentials`, `ConsentAlreadyGranted`…)
 * when the backend sends it; the UI picks the mockup text from that code.
 */
sealed interface DomainError {
    data object Network : DomainError
    data class Unauthorized(val code: String? = null) : DomainError
    data class Forbidden(val code: String? = null) : DomainError
    data class NotFound(val code: String? = null) : DomainError
    data class Conflict(val code: String? = null) : DomainError
    data class Validation(val code: String) : DomainError

    /** 5xx, 429 or a response that could not be parsed. `code` carries the status if there is no other (`HTTP_503`). */
    data class Unexpected(val code: String? = null) : DomainError
}

/** Carries a [DomainError] inside `Result.failure`. */
class DomainException(val error: DomainError) : Exception(error.toString())

/** The [DomainError] of a failed `Result`; any other exception reads as [DomainError.Unexpected]. */
fun Result<*>.domainErrorOrNull(): DomainError? = exceptionOrNull()?.let { throwable ->
    (throwable as? DomainException)?.error ?: DomainError.Unexpected()
}

fun <T> domainFailure(error: DomainError): Result<T> = Result.failure(DomainException(error))
