package pe.edu.upc.healthify.core.sync

/**
 * El usuario con sesión activa, o `null` si no hay. La cola solo envía lo que registró ese usuario.
 * Lo implementa `iam` en su infrastructure.
 */
fun interface ActiveUserIdProvider {
    suspend fun activeUserId(): Long?
}
