package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress

/**
 * Datos del login único (S4, IAM-5). La contraseña no se valida contra las reglas de registro: el backend responde
 * `InvalidCredentials` igual exista o no la cuenta (divulgación mínima).
 */
class Credentials private constructor(val email: EmailAddress, val password: String) {

    init {
        require(password.isNotEmpty()) { "Password is required" }
    }

    override fun toString(): String = "Credentials(email=${email.value}, password=****)"

    companion object {
        /**
         * Las credenciales, o `null` si el correo no tiene forma de correo o falta la contraseña. Un correo mal
         * formado se trata como credenciales incorrectas, igual que el backend (F2, caso 1).
         */
        fun parse(email: String, password: String): Credentials? {
            val address = EmailAddress.parse(email) ?: return null
            return if (password.isEmpty()) null else Credentials(address, password)
        }
    }
}
