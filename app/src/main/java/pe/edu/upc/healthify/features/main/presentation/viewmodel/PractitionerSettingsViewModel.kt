package pe.edu.upc.healthify.features.main.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.features.iam.application.usecase.ApplyAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ChangePreferredLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.main.presentation.state.PractitionerSettingsUiState
import javax.inject.Inject

sealed interface PractitionerSettingsEvent {
    /** The language was applied: before Android 13 the screen is recreated to show it. */
    data object LanguageApplied : PractitionerSettingsEvent
}

/**
 * Practitioner settings: My account, Language and Sign out. Opened from the main menu, like the patient's Settings.
 *
 * As for the patient, "My account" uses the session user stored on the phone (no need for
 * `GET /users/{uid}`).
 * As for the patient, the language is applied on tap and then saved to the account; if that fails
 * (offline, 5xx) it is retried when the connection comes back while Settings is still open. Only the UI changes:
 * clinical data and notes are not translated.
 */
@HiltViewModel
class PractitionerSettingsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAppLanguage: GetAppLanguageUseCase,
    private val applyAppLanguage: ApplyAppLanguageUseCase,
    private val changePreferredLanguage: ChangePreferredLanguageUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PractitionerSettingsUiState(language = getAppLanguage()))
    val state: StateFlow<PractitionerSettingsUiState> = _state.asStateFlow()

    private val _events = Channel<PractitionerSettingsEvent>(Channel.BUFFERED)
    val events: Flow<PractitionerSettingsEvent> = _events.receiveAsFlow()

    /** Chosen language the account has not saved yet. */
    private var pendingAccountLanguage: PreferredLanguage? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            _state.update { it.copy(fullName = user.name.fullName, email = user.email.value) }
        }
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

    /** Applied right away, then returns to Settings. */
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
