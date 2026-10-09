package pe.edu.upc.healthify.features.iam.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser

/** Sesión local del dispositivo. */
interface SessionRepository {

    /**
     * Último usuario que inició sesión aquí. Sigue disponible después de que la sesión expira, para que S6
     * («Sesión expirada Pa/Nu») sepa el rol; se borra con [logoutLocally].
     */
    val currentUser: Flow<SessionUser?>

    /** Hay usuario y tokens renovables. Pasa a `false` al expirar la sesión o al cerrarla. */
    val isLoggedIn: Flow<Boolean>

    /** Evento one-shot: el backend rechazó el refresh token (vencido o revocado). Lleva a S6. */
    val sessionExpired: Flow<Unit>

    /**
     * Cierra la sesión en el dispositivo: borra tokens y usuario. Conserva la cola de pendientes (PT21.1: «se
     * enviarán al volver a entrar»).
     */
    suspend fun logoutLocally()
}
