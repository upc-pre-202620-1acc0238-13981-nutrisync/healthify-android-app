package pe.edu.upc.healthify.features.foodcatalog.domain.valueobject

@JvmInline
value class ReferenceFoodId(val value: Long) {
    init {
        require(value > 0) { "ReferenceFoodId must be positive" }
    }
}
