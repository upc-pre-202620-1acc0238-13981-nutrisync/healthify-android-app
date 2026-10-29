package pe.edu.upc.healthify.core.sync

/**
 * The user with the active session, or `null` if there is none. The queue only sends what that user recorded.
 * Implemented by `iam` in its infrastructure.
 */
fun interface ActiveUserIdProvider {
    suspend fun activeUserId(): Long?
}
