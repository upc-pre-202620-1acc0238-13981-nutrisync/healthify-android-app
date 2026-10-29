package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * IAM-2. Token que devuelve el backend al verificar el código: fija la contraseña nueva una sola vez y vence en unos
 * minutos. Vive solo en memoria; nunca se guarda ni se loguea.
 */
@JvmInline
value class PasswordResetToken(val value: String) {

    init {
        require(value.isNotBlank()) { "Reset token is required" }
    }

    override fun toString(): String = "PasswordResetToken(****)"
}
