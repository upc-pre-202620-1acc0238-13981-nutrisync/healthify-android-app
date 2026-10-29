package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/** Patient id in CareRelationship: their account's `userId` (the backend compares `patientId` with the token). */
@JvmInline
value class PatientId(val value: Long) {
    init {
        require(value > 0) { "PatientId must be positive" }
    }
}
