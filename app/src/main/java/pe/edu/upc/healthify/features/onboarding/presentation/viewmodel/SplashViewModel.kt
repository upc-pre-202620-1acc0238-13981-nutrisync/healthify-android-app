package pe.edu.upc.healthify.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveIsLoggedInUseCase
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashEvent
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashUiState
import javax.inject.Inject

/**
 * Decides the destination only from the session saved on the device (no network; design note "Offline: same").
 * No session → welcome; session → shell loading; saved user whose session expired → session expired.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeIsLoggedIn: ObserveIsLoggedInUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    private val _events = Channel<SplashEvent>(Channel.BUFFERED)
    val events: Flow<SplashEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val destination = resolveDestination()
            _state.update { it.copy(destination = destination) }
            _events.send(SplashEvent.Navigate(destination))
        }
    }

    private suspend fun resolveDestination(): SplashDestination = try {
        val user = observeCurrentUser().first()
        when {
            user == null -> SplashDestination.Welcome
            observeIsLoggedIn().first() -> SplashDestination.ShellLoading
            // The session expired while the app was closed (the user is kept, the tokens are not) → session expired.
            else -> SplashDestination.SessionExpired
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Design note "Error: if the local read fails, go to welcome by default" (DataStore, Tink or Room).
        SplashDestination.Welcome
    }
}
