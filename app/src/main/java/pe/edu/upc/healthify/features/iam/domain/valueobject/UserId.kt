package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Account id on the backend (`users.id`, also the token's `sub`). */
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive" }
    }
}
