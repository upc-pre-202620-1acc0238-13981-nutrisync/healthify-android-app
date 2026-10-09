package pe.edu.upc.healthify.features.iam.domain.valueobject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpProblem
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpValidation
import pe.edu.upc.healthify.features.iam.domain.entity.validateNewAccount

class SignUpValueObjectsTest {

    @Test
    fun `email follows the backend pattern`() {
        assertEquals("maria.flores@correo.com", EmailAddress.parse(" Maria.Flores@Correo.com ")?.value)
        listOf("maria.flores@", "maria@correo", "maria flores@correo.com", "@correo.com", "a@b@c.com").forEach {
            assertNull(it, EmailAddress.parse(it))
        }
        assertNull(EmailAddress.parse("a".repeat(250) + "@mail.com"))
    }

    @Test
    fun `password needs upper, lower, digit, symbol and 8 to 128 characters`() {
        assertTrue(Password.isStrong("Maria123!"))
        assertTrue(Password.isStrong("Ñandú#2024"))
        mapOf(
            "short" to "Ma1!",
            "no upper" to "maria123!",
            "no lower" to "MARIA123!",
            "no digit" to "Mariaaaa!",
            "no symbol" to "Maria1234",
            "too long" to "Aa1!" + "a".repeat(125),
            "blank" to "        ",
        ).forEach { (case, raw) -> assertFalse(case, Password.isStrong(raw)) }
        assertNull(Password.parse("maria123"))
    }

    @Test
    fun `password never prints its value`() {
        assertFalse(Password("Maria123!").toString().contains("Maria123!"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weak password cannot be built`() {
        Password("maria123")
    }

    @Test
    fun `sign-up name requires both parts, collapses spaces and rejects digits or angle brackets`() {
        val name = PersonName.forSignUp("  María   José ", " Flores  Quispe")
        assertEquals("María José", name.givenNames)
        assertEquals("Flores Quispe", name.familyNames)

        assertEquals(PersonName.Problem.REQUIRED, PersonName.problemOf("   "))
        assertEquals(PersonName.Problem.INVALID_CHARACTERS, PersonName.problemOf("María2"))
        assertEquals(PersonName.Problem.INVALID_CHARACTERS, PersonName.problemOf("<b>Ana</b>"))
        assertEquals(PersonName.Problem.TOO_LONG, PersonName.problemOf("a".repeat(PersonName.MAX_LENGTH + 1)))
        assertNull(PersonName.problemOf("Ana"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sign-up name without family names fails`() {
        PersonName.forSignUp("Ana", " ")
    }

    @Test
    fun `navigation shell follows the role`() {
        assertEquals(NavigationShell.PATIENT, NavigationShell.forRole(UserRole.PATIENT))
        assertEquals(NavigationShell.PRACTITIONER, NavigationShell.forRole(UserRole.PRACTITIONER))
    }

    @Test
    fun `credentials need an email-shaped address and a password, not a strong one`() {
        assertNotNull(Credentials.parse("maria@correo.com", "abc"))
        assertNull(Credentials.parse("maria", "Maria123!"))
        assertNull(Credentials.parse("maria@correo.com", ""))
        assertFalse(Credentials.parse("maria@correo.com", "secreto")!!.toString().contains("secreto"))
    }

    @Test
    fun `valid form builds the new account`() {
        val result = validateNewAccount("María", "Flores", "maria@correo.com", "Maria123!", UserRole.PATIENT)

        val account = (result as SignUpValidation.Valid).account
        assertEquals("maria@correo.com", account.email.value)
        assertEquals(UserRole.PATIENT, account.role)
        assertEquals("Flores", account.name.familyNames)
    }

    @Test
    fun `invalid form reports every problem at once`() {
        val result = validateNewAccount("", "Flores1", "maria.flores@", "maria123", null)

        assertEquals(
            setOf(
                SignUpProblem.GivenNames(PersonName.Problem.REQUIRED),
                SignUpProblem.FamilyNames(PersonName.Problem.INVALID_CHARACTERS),
                SignUpProblem.InvalidEmail,
                SignUpProblem.WeakPassword,
                SignUpProblem.RoleNotDeclared,
            ),
            (result as SignUpValidation.Invalid).problems,
        )
    }
}
