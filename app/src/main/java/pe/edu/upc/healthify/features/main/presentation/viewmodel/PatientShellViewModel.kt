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
 * Patient shell: the main menu (☰ on the root screens) with the session user's name, the same one
 * Home and Settings use.
 */
@HiltViewModel
class PatientShellViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val requestConsultationReminders: RequestConsultationRemindersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientShellUiState())
    val state: StateFlow<PatientShellUiState> = _state.asStateFlow()

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
