package pe.edu.upc.healthify.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveSessionExpiredUseCase
import javax.inject.Inject

/** App state that only the root graph cares about: the "session expired" notice (refresh rejected). */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    observeSessionExpired: ObserveSessionExpiredUseCase,
) : ViewModel() {

    /** Sole collector of [ObserveSessionExpiredUseCase] (CONFLATED channel: it waits while the app is in background). */
    val sessionExpired: Flow<Unit> = observeSessionExpired()
}
