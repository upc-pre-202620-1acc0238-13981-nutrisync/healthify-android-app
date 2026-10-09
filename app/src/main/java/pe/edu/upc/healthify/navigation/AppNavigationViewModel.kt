package pe.edu.upc.healthify.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveSessionExpiredUseCase
import javax.inject.Inject

/** Estado de la app que solo le importa al grafo raíz: el aviso «la sesión expiró» (refresh rechazado) → S6. */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    observeSessionExpired: ObserveSessionExpiredUseCase,
) : ViewModel() {

    /** Único colector de [ObserveSessionExpiredUseCase] (el canal es CONFLATED: espera si la app está en fondo). */
    val sessionExpired: Flow<Unit> = observeSessionExpired()
}
