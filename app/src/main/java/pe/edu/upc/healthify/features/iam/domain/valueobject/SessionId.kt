package pe.edu.upc.healthify.features.iam.domain.valueobject

/** Id de la `UserSession` del backend (claim `sessionId`); con él se pide el navigation shell (S5). */
@JvmInline
value class SessionId(val value: Long) {
    init {
        require(value > 0) { "SessionId must be positive" }
    }
}
