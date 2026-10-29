package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * IAM-2. Código de recuperación que llega por correo: exactamente seis dígitos. El backend decide si es correcto;
 * aquí solo se evita enviar algo que no puede serlo.
 */
@JvmInline
value class PasswordResetCode private constructor(val value: String) {

    /** Nunca exponer el código en logs ni en el estado de la UI. */
    override fun toString(): String = "PasswordResetCode(******)"

    companion object {
        const val LENGTH = 6

        /** El código, o `null` si [raw] (sin espacios) no son seis dígitos. */
        fun parse(raw: String): PasswordResetCode? {
            val digits = raw.filterNot { it.isWhitespace() }
            return if (digits.length == LENGTH && digits.all { it in '0'..'9' }) PasswordResetCode(digits) else null
        }
    }
}
