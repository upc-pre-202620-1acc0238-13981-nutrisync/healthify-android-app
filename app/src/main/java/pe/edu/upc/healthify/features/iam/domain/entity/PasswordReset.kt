package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.Password
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken

/**
 * Data to set a new password with the token returned by the code verification. The password follows
 * the sign-up rules ([Password]): the backend answers `WeakPassword` with the same rules, and `PasswordReuseNotAllowed`
 * if it is the current one.
 */
data class PasswordReset(
    val token: PasswordResetToken,
    val newPassword: Password,
)
