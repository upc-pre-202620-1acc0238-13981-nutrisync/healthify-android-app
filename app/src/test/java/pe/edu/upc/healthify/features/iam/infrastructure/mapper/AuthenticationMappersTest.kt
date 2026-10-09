package pe.edu.upc.healthify.features.iam.infrastructure.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.SignUpValidation
import pe.edu.upc.healthify.features.iam.domain.entity.validateNewAccount
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.NavigationShellDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignInRequestDto
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.SignUpRequestDto

class AuthenticationMappersTest {

    @Test
    fun `new account maps to SignUpResource with the backend role name`() {
        val account = (
            validateNewAccount(" María José ", "Flores", "Maria@Correo.com", "Maria123!", UserRole.PRACTITIONER)
                as SignUpValidation.Valid
            ).account

        assertEquals(
            SignUpRequestDto(
                email = "maria@correo.com",
                password = "Maria123!",
                role = "Practitioner",
                givenNames = "María José",
                familyNames = "Flores",
            ),
            account.toDto(),
        )
    }

    @Test
    fun `credentials map to SignInResource without role`() {
        assertEquals(
            SignInRequestDto("maria@correo.com", "x"),
            Credentials.parse("MARIA@correo.com", "x")!!.toDto(),
        )
    }

    @Test
    fun `navigation shell maps both shells and reads missing or terminated as null`() {
        assertEquals(NavigationShell.PATIENT, NavigationShellDto(3, "Patient", "PatientShell", true).toDomain())
        assertEquals(
            NavigationShell.PRACTITIONER,
            NavigationShellDto(3, "Practitioner", "PractitionerShell", true).toDomain(),
        )
        assertNull(NavigationShellDto(3, "Patient", null, true).toDomain())
        assertNull(NavigationShellDto(3, null, "PatientShell", false).toDomain())
        assertNull(NavigationShellDto(3, "Patient", "AdminShell", true).toDomain())
    }
}
