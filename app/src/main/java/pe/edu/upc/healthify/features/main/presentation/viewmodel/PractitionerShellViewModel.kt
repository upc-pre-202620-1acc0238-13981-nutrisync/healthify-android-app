package pe.edu.upc.healthify.features.main.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.main.presentation.state.PractitionerShellUiState
import javax.inject.Inject

/**
 * Shell del nutricionista: el menú principal (PR1.D, ☰ de las pestañas) con el nombre del usuario de la sesión, el
 * mismo que usa Ajustes.
 */
@HiltViewModel
class PractitionerShellViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PractitionerShellUiState())
    val state: StateFlow<PractitionerShellUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeCurrentUser().collect { user ->
                _state.update {
                    it.copy(
                        fullName = user?.name?.fullName.orEmpty(),
                        avatarInitial = user?.name?.initial?.toString().orEmpty(),
                    )
                }
            }
        }
    }

    fun onOpenMenu() = _state.update { it.copy(isMenuOpen = true) }

    fun onCloseMenu() = _state.update { it.copy(isMenuOpen = false) }
}
