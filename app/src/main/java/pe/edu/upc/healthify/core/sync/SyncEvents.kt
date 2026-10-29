package pe.edu.upc.healthify.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signals that the backend accepted operations from the queue. What was sent is already on the server, but the
 * screens that show it read earlier (when the connection comes back, the read usually lands before the send): this
 * signal makes them read again. Only the feature repositories use it, filtered by their type.
 */
@Singleton
class SyncEvents @Inject constructor() {

    private val delivered = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** One emission per batch with at least one accepted operation of [type]. */
    fun deliveredOf(type: String): Flow<Unit> = delivered.asSharedFlow().filter { it == type }.map { }

    fun notifyDelivered(type: String) {
        delivered.tryEmit(type)
    }

    private companion object {
        const val BUFFER = 16
    }
}
