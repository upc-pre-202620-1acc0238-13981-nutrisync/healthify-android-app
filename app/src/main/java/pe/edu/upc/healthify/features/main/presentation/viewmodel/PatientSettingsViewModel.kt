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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ApplyAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ChangePreferredLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.main.presentation.state.PatientSettingsUiState
import javax.inject.Inject

sealed interface PatientSettingsEvent {
    /** El idioma se aplicó: antes de Android 13 la pantalla se recrea para verlo. */
    data object LanguageApplied : PatientSettingsEvent
}

/**
 * PT21 · Ajustes: compone Iam (cuenta, idioma) y CareRelationship (estado de las funciones con IA).
 *
 * DECISIÓN PT21: «Mi cuenta» usa el usuario de la sesión guardado en el teléfono (no hace falta `GET /users/{uid}`).
 * DECISIÓN PT21.I: el idioma se aplica al tocarlo y luego se guarda en la cuenta (IAM-3); si eso falla (sin red, 5xx)
 * se reintenta al volver la conexión mientras siga en Ajustes.
 */
@HiltViewModel
class PatientSettingsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val getAppLanguage: GetAppLanguageUseCase,
    private val applyAppLanguage: ApplyAppLanguageUseCase,
    private val changePreferredLanguage: ChangePreferredLanguageUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientSettingsUiState(language = getAppLanguage()))
    val state: StateFlow<PatientSettingsUiState> = _state.asStateFlow()

    private val _events = Channel<PatientSettingsEvent>(Channel.BUFFERED)
    val events: Flow<PatientSettingsEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null

    /** Idioma elegido que la cuenta todavía no guardó. */
    private var pendingAccountLanguage: PreferredLanguage? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            _state.update { it.copy(fullName = user.name.fullName, email = user.email.value) }
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) {
                        pendingAccountLanguage?.let { saveAccountLanguage(it) }
                        refreshAiStatus()
                    }
                }
            }
            refreshAiStatus()
        }
    }

    /** Al volver de PT21.IA el estado de la IA pudo cambiar. */
    fun onResume() {
        _state.update { it.copy(language = getAppLanguage()) }
        viewModelScope.launch { refreshAiStatus() }
    }

    fun onOpenLanguage() = _state.update { it.copy(showLanguageSheet = true) }

    fun onDismissLanguage() = _state.update { it.copy(showLanguageSheet = false) }

    /** PT21.I: se aplica de inmediato y vuelve a Ajustes. */
    fun onLanguageSelected(language: PreferredLanguage) {
        _state.update { it.copy(showLanguageSheet = false, language = language) }
        val applied = applyAppLanguage(language)
        viewModelScope.launch {
            if (applied) _events.send(PatientSettingsEvent.LanguageApplied)
            saveAccountLanguage(language)
        }
    }

    /** «Descargar una copia de mis datos» → «Próximamente» (DECISIÓN PT21: RM-6 no existe en el backend). */
    fun onDataExport() = _state.update { it.copy(showDataExportDialog = true) }

    fun onDismissDataExport() = _state.update { it.copy(showDataExportDialog = false) }

    private suspend fun saveAccountLanguage(language: PreferredLanguage) {
        val saved = changePreferredLanguage(language).isSuccess
        pendingAccountLanguage = if (saved) null else language
    }

    private suspend fun refreshAiStatus() {
        val pid = patientId ?: return
        val preferences = getAiPreferences(pid)
        _state.update { it.copy(aiFeaturesActive = preferences.anyFeatureAvailable) }
    }
}
