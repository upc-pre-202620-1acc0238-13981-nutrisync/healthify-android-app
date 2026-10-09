package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/** Id del paciente en CareRelationship: es el `userId` de su cuenta (el backend compara `patientId` con el token). */
@JvmInline
value class PatientId(val value: Long) {
    init {
        require(value > 0) { "PatientId must be positive" }
    }
}
