package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.Password
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole

/** An account to create. Signing up grants access to nothing: clinical access comes with the care link. */
data class NewAccount(
    val name: PersonName,
    val email: EmailAddress,
    val password: Password,
    val role: UserRole,
)

/** A sign-up form field with a problem, with its matching backend code. */
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

/** Result of validating the sign-up form: the account ready to send, or all its problems. */
sealed interface SignUpValidation {
    data class Valid(val account: NewAccount) : SignUpValidation
    data class Invalid(val problems: Set<SignUpProblem>) : SignUpValidation
}

/** Validates the whole form with the value object rules (the same as the backend's). */
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
