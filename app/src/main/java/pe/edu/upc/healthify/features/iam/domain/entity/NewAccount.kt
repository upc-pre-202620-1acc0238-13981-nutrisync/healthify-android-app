package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.Password
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole

/** Una cuenta por crear (S3, F1). Registrarse no da acceso a nada: el acceso clínico llega con el vínculo. */
data class NewAccount(
    val name: PersonName,
    val email: EmailAddress,
    val password: Password,
    val role: UserRole,
)

/** Un campo del formulario de registro con un problema, con el código del backend que le corresponde. */
sealed interface SignUpProblem {
    data class GivenNames(val problem: PersonName.Problem) : SignUpProblem
    data class FamilyNames(val problem: PersonName.Problem) : SignUpProblem

    /** `InvalidEmail`. */
    data object InvalidEmail : SignUpProblem

    /** `WeakPassword`. */
    data object WeakPassword : SignUpProblem

    /** `RoleNotDeclared`. */
    data object RoleNotDeclared : SignUpProblem
}

/** Resultado de validar el formulario de registro: la cuenta lista para enviar o todos sus problemas. */
sealed interface SignUpValidation {
    data class Valid(val account: NewAccount) : SignUpValidation
    data class Invalid(val problems: Set<SignUpProblem>) : SignUpValidation
}

/** Valida el formulario completo con las reglas de los value objects (las mismas del backend). */
fun validateNewAccount(
    givenNames: String,
    familyNames: String,
    email: String,
    password: String,
    role: UserRole?,
): SignUpValidation {
    val problems = buildSet {
        PersonName.problemOf(givenNames)?.let { add(SignUpProblem.GivenNames(it)) }
        PersonName.problemOf(familyNames)?.let { add(SignUpProblem.FamilyNames(it)) }
        if (EmailAddress.parse(email) == null) add(SignUpProblem.InvalidEmail)
        if (!Password.isStrong(password)) add(SignUpProblem.WeakPassword)
        if (role == null) add(SignUpProblem.RoleNotDeclared)
    }
    if (problems.isNotEmpty() || role == null) return SignUpValidation.Invalid(problems)
    return SignUpValidation.Valid(
        NewAccount(
            name = PersonName.forSignUp(givenNames, familyNames),
            email = EmailAddress(email),
            password = Password(password),
            role = role,
        ),
    )
}
