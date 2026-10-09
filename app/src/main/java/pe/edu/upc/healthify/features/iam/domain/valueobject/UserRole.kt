package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Rol de la cuenta. Se declara al registrarse y es inmutable (F1: «una cuenta = un rol»); decide el shell
 * (paciente o nutricionista).
 */
enum class UserRole { PATIENT, PRACTITIONER }
