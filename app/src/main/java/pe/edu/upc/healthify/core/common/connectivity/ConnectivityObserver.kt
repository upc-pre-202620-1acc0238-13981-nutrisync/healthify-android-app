package pe.edu.upc.healthify.core.common.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Connection state for the offline banners (`OfflineBanner`) and the Figma offline states.
 * Pure Kotlin: ViewModels inject it without knowing about `ConnectivityManager`.
 */
interface ConnectivityObserver {
    /** Emits the current state on subscription and then every change (no repeats). */
    val isOnline: Flow<Boolean>
}
