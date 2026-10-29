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
import pe.edu.upc.healthify.features.main.presentation.state.PatientShellUiState
import pe.edu.upc.healthify.features.monitoring.application.usecase.RequestConsultationRemindersUseCase
import javax.inject.Inject

/**
 * Shell del paciente: el menú principal (☰ de las pantallas raíz) con el nombre del usuario de la sesión, el mismo
 * que usan Inicio y Ajustes.
 */
@HiltViewModel
class PatientShellViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val requestConsultationReminders: RequestConsultationRemindersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientShellUiState())
    val state: StateFlow<PatientShellUiState> = _state.asStateFlow()

    init {
        // Al entrar al shell se reprograman los avisos de las próximas consultas (también cambian desde otro teléfono).
        requestConsultationReminders()
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
