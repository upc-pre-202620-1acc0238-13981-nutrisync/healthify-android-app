package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/** Id of a care link (`CareLinkResource.careLinkId`). */
@JvmInline
value class CareLinkId(val value: Long) {
    init {
        require(value > 0) { "CareLinkId must be positive" }
    }
}
