package pe.edu.upc.healthify.core.common.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Estado de la conexión para los banners «Sin conexión» (`OfflineBanner`) y los estados offline del Figma.
 * Kotlin puro: los ViewModels lo inyectan sin conocer `ConnectivityManager`.
 */
interface ConnectivityObserver {
    /** Emite el estado actual al suscribirse y luego cada cambio (sin repetidos). */
    val isOnline: Flow<Boolean>
}
