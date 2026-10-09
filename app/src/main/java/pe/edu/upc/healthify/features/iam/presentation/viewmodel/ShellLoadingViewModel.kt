package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientLinkStatusUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ResolveNavigationShellUseCase
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.presentation.state.ShellDestination
import pe.edu.upc.healthify.features.iam.presentation.state.ShellLoadingEvent
import pe.edu.upc.healthify.features.iam.presentation.state.ShellLoadingUiState
import pe.edu.upc.healthify.features.iam.presentation.state.selectShellDestination
import javax.inject.Inject

/**
 * S5 · Cargando. Compone Iam (shell de la sesión) y CareRelationship (estado del vínculo del paciente).
 * Nota S5: «si la selección de shell no responde, se reintenta en silencio; si persiste, se muestra este estado
 * con Reintentar».
 */
@HiltViewModel
class ShellLoadingViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val resolveNavigationShell: ResolveNavigationShellUseCase,
    private val getPatientLinkStatus: GetPatientLinkStatusUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ShellLoadingUiState())
    val state: StateFlow<ShellLoadingUiState> = _state.asStateFlow()

    private val _events = Channel<ShellLoadingEvent>(Channel.BUFFERED)
    val events: Flow<ShellLoadingEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() {
        if (_state.value.isRetrying) return
        _state.update { it.copy(isRetrying = true) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _events.send(ShellLoadingEvent.NavigateToWelcome)
                return@launch
            }
            val destination = resolveDestination(user) ?: run {
                delay(SILENT_RETRY_DELAY_MS)
                resolveDestination(user)
            }
            if (destination == null) {
                _state.update { ShellLoadingUiState(isError = true) }
            } else {
                _events.send(ShellLoadingEvent.Navigate(destination))
            }
        }
    }

    /** `null` si algo falló (se reintenta o se muestra el error). */
    private suspend fun resolveDestination(user: SessionUser): ShellDestination? {
        val shell = resolveNavigationShell(user).getOrElse { return null }
        val linkStatus: PatientLinkStatus? = if (shell == NavigationShell.PATIENT) {
            getPatientLinkStatus(user.id.value).getOrElse { return null }
        } else {
            null
        }
        return selectShellDestination(shell, linkStatus)
    }

    private companion object {
        const val SILENT_RETRY_DELAY_MS = 1_000L
    }
}
