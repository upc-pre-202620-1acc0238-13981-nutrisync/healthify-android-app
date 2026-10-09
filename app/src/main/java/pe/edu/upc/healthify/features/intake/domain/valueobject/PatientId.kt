package pe.edu.upc.healthify.features.intake.domain.valueobject

/** Id del paciente en IntakeBodyResponse: es el `userId` de su cuenta (el backend lo compara con el token). */
@JvmInline
value class PatientId(val value: Long) {
    init {
        require(value > 0) { "PatientId must be positive" }
    }
}
