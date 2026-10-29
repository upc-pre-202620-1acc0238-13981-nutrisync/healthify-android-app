package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot "session expired" event (the refresh token was rejected). The root graph consumes it to open the
 * session expired screen. Conflated: several requests failing at once leave a single pending event.
 */
@Singleton
class SessionExpiryNotifier @Inject constructor() {

    private val channel = Channel<Unit>(Channel.CONFLATED)

    val events: Flow<Unit> = channel.receiveAsFlow()

    fun notifyExpired() {
        channel.trySend(Unit)
    }
}
