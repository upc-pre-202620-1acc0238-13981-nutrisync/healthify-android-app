package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress

/**
 * Single sign-in data. The password is not checked against the sign-up rules: the backend answers
 * `InvalidCredentials` whether or not the account exists (minimal disclosure).
 */
class Credentials private constructor(val email: EmailAddress, val password: String) {

    init {
        require(password.isNotEmpty()) { "Password is required" }
    }

    override fun toString(): String = "Credentials(email=${email.value}, password=****)"

    companion object {
        /**
         * The credentials, or `null` if the email is not shaped like an email or the password is missing. A malformed
         * email is treated as wrong credentials, the same as the backend does.
         */
        fun parse(email: String, password: String): Credentials? {
            val address = EmailAddress.parse(email) ?: return null
            return if (password.isEmpty()) null else Credentials(address, password)
        }
    }
}
