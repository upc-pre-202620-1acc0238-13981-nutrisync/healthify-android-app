package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Contraseña de una cuenta nueva, con las reglas del VO `Password` del backend (F1, `WeakPassword`): de 8 a 128
 * caracteres, con al menos una mayúscula, una minúscula, un dígito y un carácter que no sea letra ni dígito.
 *
 * Solo se usa al registrarse: el inicio de sesión envía lo que la persona escribió (ver [Credentials]).
 */
@JvmInline
value class Password private constructor(val value: String) {

    /** Nunca exponer la contraseña en logs ni en el estado de la UI. */
    override fun toString(): String = "Password(****)"

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 128

        /** @throws IllegalArgumentException si [raw] no cumple las reglas. */
        operator fun invoke(raw: String): Password {
            require(isStrong(raw)) { "Weak password" }
            return Password(raw)
        }

        /** La contraseña, o `null` si [raw] no cumple las reglas. */
        fun parse(raw: String): Password? = if (isStrong(raw)) Password(raw) else null

        fun isStrong(raw: String): Boolean =
            raw.isNotBlank() &&
                raw.length in MIN_LENGTH..MAX_LENGTH &&
                raw.any { it.isUpperCase() } &&
                raw.any { it.isLowerCase() } &&
                raw.any { it.isDigit() } &&
                !raw.all { it.isLetterOrDigit() }
    }
}
