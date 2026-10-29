package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Account role. Declared at sign-up and immutable ("one account = one role"); it decides the shell
 * (patient or practitioner).
 */
enum class UserRole { PATIENT, PRACTITIONER }
