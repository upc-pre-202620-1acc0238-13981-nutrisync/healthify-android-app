package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Id de la cuenta en el backend (`users.id`, también el `sub` del token). */
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive" }
    }
}
