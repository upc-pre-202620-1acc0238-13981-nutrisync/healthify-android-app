package pe.edu.upc.healthify.core.designsystem.component

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.common.error.DomainError

/**
 * Texto genérico por tipo de error. Úsalo solo como respaldo: cada pantalla mapea primero los
 * códigos que su frame «Notas» documenta a sus propios strings del mockup.
 */
@StringRes
fun DomainError.genericMessageRes(): Int = when (this) {
    DomainError.Network -> R.string.error_network
    is DomainError.Unauthorized -> R.string.error_unauthorized
    is DomainError.Forbidden -> R.string.error_forbidden
    is DomainError.NotFound -> R.string.error_not_found
    is DomainError.Conflict -> R.string.error_conflict
    is DomainError.Validation -> R.string.error_validation
    is DomainError.Unexpected -> R.string.error_unexpected
}
