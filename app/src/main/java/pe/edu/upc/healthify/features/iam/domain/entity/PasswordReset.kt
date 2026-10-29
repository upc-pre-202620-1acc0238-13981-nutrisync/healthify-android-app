package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.Password
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken

/**
 * IAM-2. Datos para fijar una contraseña nueva con el token que dio la verificación del código. La contraseña sigue
 * las reglas de registro ([Password]): el backend responde `WeakPassword` con las mismas, y `PasswordReuseNotAllowed`
 * si es la actual.
 */
data class PasswordReset(
    val token: PasswordResetToken,
    val newPassword: Password,
)
