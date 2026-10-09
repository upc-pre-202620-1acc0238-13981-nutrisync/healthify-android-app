package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ChangeReminderUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveReminderSettingsUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.presentation.state.RemindersUiState
import javax.inject.Inject

/**
 * PT22: los cambios se guardan al vuelo. El permiso de notificaciones lo pide la pantalla antes de encender uno; si se
 * niega, el recordatorio queda apagado y se muestra PT22.M (DECISIÓN PT22: no se guarda encendido algo que no avisaría).
 */
@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val observeReminderSettings: ObserveReminderSettingsUseCase,
    private val changeReminder: ChangeReminderUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RemindersUiState())
    val state: StateFlow<RemindersUiState> = _state.asStateFlow()

    private var patientId: Long? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            observeReminderSettings(user.id.value).collect { settings ->
                _state.update { it.copy(isLoading = false, settings = settings) }
            }
        }
    }

    /** Llamar solo con el permiso ya concedido cuando [enabled] es `true`. */
    fun onToggle(kind: ReminderKind, enabled: Boolean) {
        val pid = patientId ?: return
        _state.update { it.copy(settings = it.settings.with(kind, enabled)) }
        viewModelScope.launch { changeReminder(pid, kind, enabled) }
    }

    fun onPermissionDenied() {
        _state.update { it.copy(showPermissionDialog = true) }
    }

    fun onDismissPermissionDialog() {
        _state.update { it.copy(showPermissionDialog = false) }
    }
}
