package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

/** Patient id in NutritionalCare: the `userId` of their account (the backend compares it with the token). */
@JvmInline
value class PatientId(val value: Long) {
    init {
        require(value > 0) { "PatientId must be positive" }
    }
}
