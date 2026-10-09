package pe.edu.upc.healthify.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisa que el backend aceptó operaciones de la cola. Lo que se envió ya está en el servidor, pero las pantallas que
 * lo muestran leyeron antes (al volver la conexión, la lectura suele llegar antes que el envío): con este aviso
 * vuelven a leer. Solo lo usan los repositorios de las features, filtrado por su tipo.
 */
@Singleton
class SyncEvents @Inject constructor() {

    private val delivered = MutableSharedFlow<String>(extraBufferCapacity = BUFFER)

    /** Una emisión por lote con al menos una operación aceptada de [type]. */
    fun deliveredOf(type: String): Flow<Unit> = delivered.asSharedFlow().filter { it == type }.map { }

    fun notifyDelivered(type: String) {
        delivered.tryEmit(type)
    }

    private companion object {
        const val BUFFER = 16
    }
}
