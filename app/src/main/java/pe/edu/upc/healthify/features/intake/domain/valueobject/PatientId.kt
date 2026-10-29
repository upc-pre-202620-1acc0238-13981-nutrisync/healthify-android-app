package pe.edu.upc.healthify.features.intake.domain.valueobject

/** Patient id in IntakeBodyResponse: it is their account's `userId` (the backend compares it with the token). */
@JvmInline
value class PatientId(val value: Long) {
    init {
        require(value > 0) { "PatientId must be positive" }
    }
}
