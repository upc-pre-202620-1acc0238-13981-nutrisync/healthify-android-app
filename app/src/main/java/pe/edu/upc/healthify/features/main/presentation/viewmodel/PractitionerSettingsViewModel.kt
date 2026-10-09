package pe.edu.upc.healthify.features.main.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.iam.application.usecase.ApplyAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ChangePreferredLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import javax.inject.Inject

/** PR20 · Ajustes del nutricionista (+ PR20.I hoja de idioma). Menú estático. */
data class PractitionerSettingsUiState(
    val language: PreferredLanguage = PreferredLanguage.DEFAULT,
    val isOffline: Boolean = false,
    val showLanguageSheet: Boolean = false,
)

sealed interface PractitionerSettingsEvent {
    /** El idioma se aplicó: antes de Android 13 la pantalla se recrea para verlo. */
    data object LanguageApplied : PractitionerSettingsEvent
}

/**
 * PR20 · Ajustes: Catálogo de alimentos, Idioma y Cerrar sesión.
 *
 * DECISIÓN PR20: el frame no muestra datos de la cuenta, así que no se pide `GET /users/{uid}`.
 * DECISIÓN PR20.I: como PT21.I, el idioma se aplica al tocarlo y luego se guarda en la cuenta (IAM-3); si eso falla
 * (sin red, 5xx) se reintenta al volver la conexión mientras siga en Ajustes. Cambia solo la interfaz: los datos
 * clínicos y las notas no se traducen.
 */
@HiltViewModel
class PractitionerSettingsViewModel @Inject constructor(
    private val getAppLanguage: GetAppLanguageUseCase,
    private val applyAppLanguage: ApplyAppLanguageUseCase,
    private val changePreferredLanguage: ChangePreferredLanguageUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PractitionerSettingsUiState(language = getAppLanguage()))
    val state: StateFlow<PractitionerSettingsUiState> = _state.asStateFlow()

    private val _events = Channel<PractitionerSettingsEvent>(Channel.BUFFERED)
    val events: Flow<PractitionerSettingsEvent> = _events.receiveAsFlow()

    /** Idioma elegido que la cuenta todavía no guardó. */
    private var pendingAccountLanguage: PreferredLanguage? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) pendingAccountLanguage?.let { saveAccountLanguage(it) }
            }
        }
    }

    fun onResume() = _state.update { it.copy(language = getAppLanguage()) }

    fun onOpenLanguage() = _state.update { it.copy(showLanguageSheet = true) }

    fun onDismissLanguage() = _state.update { it.copy(showLanguageSheet = false) }

    /** PR20.I: se aplica de inmediato y vuelve a Ajustes. */
    fun onLanguageSelected(language: PreferredLanguage) {
        _state.update { it.copy(showLanguageSheet = false, language = language) }
        val applied = applyAppLanguage(language)
        viewModelScope.launch {
            if (applied) _events.send(PractitionerSettingsEvent.LanguageApplied)
            saveAccountLanguage(language)
        }
    }

    private suspend fun saveAccountLanguage(language: PreferredLanguage) {
        val saved = changePreferredLanguage(language).isSuccess
        pendingAccountLanguage = if (saved) null else language
    }
}
