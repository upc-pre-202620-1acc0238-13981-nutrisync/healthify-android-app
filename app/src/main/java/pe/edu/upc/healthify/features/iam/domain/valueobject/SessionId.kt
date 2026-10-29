package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Id of the backend `UserSession` (`sessionId` claim); used to request the navigation shell. */
@JvmInline
value class SessionId(val value: Long) {
    init {
        require(value > 0) { "SessionId must be positive" }
    }
}
