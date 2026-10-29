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
import pe.edu.upc.healthify.features.monitoring.application.usecase.RequestConsultationRemindersUseCase
import javax.inject.Inject

/**
 * Practitioner shell: the main menu (☰ on the tabs) with the session user's name, the same one
 * Settings uses.
 */
@HiltViewModel
class PractitionerShellViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val requestConsultationReminders: RequestConsultationRemindersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PractitionerShellUiState())
    val state: StateFlow<PractitionerShellUiState> = _state.asStateFlow()

    init {
        // On entering the shell, upcoming appointment reminders are rescheduled (they can also change from another phone).
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
