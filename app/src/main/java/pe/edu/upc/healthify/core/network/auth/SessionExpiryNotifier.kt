package pe.edu.upc.healthify.core.network.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evento one-shot «la sesión expiró» (el refresh token fue rechazado). Lo consume el grafo raíz para ir a S6.
 * Conflated: varias peticiones que fallan a la vez dejan un solo evento pendiente.
 */
@Singleton
class SessionExpiryNotifier @Inject constructor() {

    private val channel = Channel<Unit>(Channel.CONFLATED)

    val events: Flow<Unit> = channel.receiveAsFlow()

    fun notifyExpired() {
        channel.trySend(Unit)
    }
}
