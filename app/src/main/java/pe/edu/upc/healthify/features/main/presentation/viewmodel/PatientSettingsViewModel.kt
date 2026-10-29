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
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ApplyAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ChangePreferredLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.GetAppLanguageUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.main.application.usecase.DownloadDataExportUseCase
import pe.edu.upc.healthify.features.main.application.usecase.GetDataExportFileNameUseCase
import pe.edu.upc.healthify.features.main.domain.valueobject.DataExportFileName
import pe.edu.upc.healthify.features.main.presentation.state.DataExportStatus
import pe.edu.upc.healthify.features.main.presentation.state.PatientSettingsUiState
import javax.inject.Inject

sealed interface PatientSettingsEvent {
    /** The language was applied: before Android 13 the screen is recreated to show it. */
    data object LanguageApplied : PatientSettingsEvent

    /** Open `CreateDocument("application/zip")` with this suggested name. */
    data class PickDataExportDestination(val fileName: DataExportFileName) : PatientSettingsEvent
}

/**
 * Patient settings: combines Iam (account, language), CareRelationship (AI features status) and the data copy
 * download (ZIP of CSV files, written to the document the person picks).
 *
 * "My account" uses the session user stored on the phone (no need for `GET /users/{uid}`).
 * The language is applied on tap and then saved to the account; if that fails (offline, 5xx)
 * it is retried when the connection comes back while Settings is still open.
 */
@HiltViewModel
class PatientSettingsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val getAppLanguage: GetAppLanguageUseCase,
    private val applyAppLanguage: ApplyAppLanguageUseCase,
    private val changePreferredLanguage: ChangePreferredLanguageUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val getDataExportFileName: GetDataExportFileNameUseCase,
    private val downloadDataExport: DownloadDataExportUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientSettingsUiState(language = getAppLanguage()))
    val state: StateFlow<PatientSettingsUiState> = _state.asStateFlow()

    private val _events = Channel<PatientSettingsEvent>(Channel.BUFFERED)
    val events: Flow<PatientSettingsEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null

    /** Chosen language the account has not saved yet. */
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

    /** On returning from the AI features screen the AI status may have changed. */
    fun onResume() {
        _state.update { it.copy(language = getAppLanguage()) }
        viewModelScope.launch { refreshAiStatus() }
    }

    fun onOpenLanguage() = _state.update { it.copy(showLanguageSheet = true) }

    fun onDismissLanguage() = _state.update { it.copy(showLanguageSheet = false) }

    /** Applied right away, then returns to Settings. */
    fun onLanguageSelected(language: PreferredLanguage) {
        _state.update { it.copy(showLanguageSheet = false, language = language) }
        val applied = applyAppLanguage(language)
        viewModelScope.launch {
            if (applied) _events.send(PatientSettingsEvent.LanguageApplied)
            saveAccountLanguage(language)
        }
    }

    /**
     * "Download a copy of my data": asks the screen to open the file picker with the suggested name.
     * Offline it does nothing (the item already shows as disabled and explains a connection is needed).
     */
    fun onDataExport() {
        if (_state.value.isOffline || _state.value.dataExport == DataExportStatus.InProgress) return
        viewModelScope.launch { _events.send(PatientSettingsEvent.PickDataExportDestination(getDataExportFileName())) }
    }

    /** The picker returned the chosen document; `null` if the person cancelled. */
    fun onDataExportDestinationPicked(destinationUri: String?) {
        val pid = patientId
        if (destinationUri == null || pid == null) return
        _state.update { it.copy(dataExport = DataExportStatus.InProgress) }
        viewModelScope.launch {
            val result = downloadDataExport(pid, destinationUri)
            val status = if (result.isSuccess) {
                DataExportStatus.Saved
            } else {
                DataExportStatus.Failed(offline = result.domainErrorOrNull() == DomainError.Network)
            }
            _state.update { it.copy(dataExport = status) }
        }
    }

    fun onDismissDataExport() = _state.update { it.copy(dataExport = DataExportStatus.Idle) }

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
