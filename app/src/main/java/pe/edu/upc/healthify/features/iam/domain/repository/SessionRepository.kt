package pe.edu.upc.healthify.features.iam.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser

/** Local session on the device. */
interface SessionRepository {

    /**
     * Last user who signed in here. Still available after the session expires, so the session expired screen
     * knows the role; cleared by [logoutLocally].
     */
    val currentUser: Flow<SessionUser?>

    /** There is a user and refreshable tokens. Turns `false` when the session expires or is closed. */
    val isLoggedIn: Flow<Boolean>

    /** One-shot event: the backend rejected the refresh token (expired or revoked); shows the session expired screen. */
    val sessionExpired: Flow<Unit>

    /**
     * Clears the session on the device: deletes tokens and user. Keeps the pending queue (it is "sent when you
     * sign in again").
     */
    suspend fun logoutLocally()
}
