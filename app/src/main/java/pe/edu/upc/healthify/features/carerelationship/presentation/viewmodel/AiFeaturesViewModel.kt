package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.ChangeAiFeatureUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GrantAiProcessingConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.presentation.state.AiFeaturesUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentOffer
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import javax.inject.Inject

sealed interface AiFeaturesEvent {
    /** Snackbar: el cambio no se guardó (sin red o error del servidor); el interruptor vuelve a como estaba. */
    data class ShowSaveFailed(val offline: Boolean) : AiFeaturesEvent
}

/**
 * PT21.IA: el paciente decide qué funciones con IA usa. Encender una sin el consentimiento de IA (CR-2) ofrece
 * activarlo primero; al aceptarlo se otorga (`PUT /care-links/{id}/ai-processing-consent`) y luego se enciende la
 * función. Apagar nunca pide nada más: no cambia el plan ni los registros.
 */
@HiltViewModel
class AiFeaturesViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val changeAiFeature: ChangeAiFeatureUseCase,
    private val grantAiProcessingConsent: GrantAiProcessingConsentUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(AiFeaturesUiState())
    val state: StateFlow<AiFeaturesUiState> = _state.asStateFlow()

    private val _events = Channel<AiFeaturesEvent>(Channel.BUFFERED)
    val events: Flow<AiFeaturesEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) refresh()
                }
            }
            refresh()
        }
    }

    fun onToggle(feature: AiFeature, enabled: Boolean) {
        val current = _state.value
        val pid = patientId ?: return
        if (!current.canChange) return
        if (enabled && !current.consentGranted) {
            _state.update { it.copy(consentOffer = ConsentOffer(feature)) }
            return
        }
        save(pid, feature, enabled)
    }

    /** «Activar funciones con IA» de la card sin consentimiento. */
    fun onActivateConsent() {
        if (!_state.value.canChange) return
        _state.update { it.copy(consentOffer = ConsentOffer(feature = null)) }
    }

    fun onDismissConsent() {
        _state.update { it.copy(consentOffer = null) }
    }

    fun onConfirmConsent() {
        val pid = patientId ?: return
        val offer = _state.value.consentOffer ?: return
        if (_state.value.isGrantingConsent) return
        _state.update { it.copy(isGrantingConsent = true) }
        viewModelScope.launch {
            val result = grantAiProcessingConsent(pid)
            val preferences = result.getOrNull()
            if (preferences == null) {
                _state.update { it.copy(isGrantingConsent = false, consentOffer = null) }
                _events.send(AiFeaturesEvent.ShowSaveFailed(offline = result.domainErrorOrNull() == DomainError.Network))
                return@launch
            }
            _state.update { it.copy(isGrantingConsent = false, consentOffer = null, preferences = preferences) }
            offer.feature?.let { save(pid, it, enabled = true) }
        }
    }

    private fun save(pid: Long, feature: AiFeature, enabled: Boolean) {
        val before = _state.value.preferences
        // Optimista: el interruptor cambia al tocarlo y vuelve si el backend no lo acepta.
        _state.update { it.copy(savingFeature = feature, preferences = before.with(feature, enabled)) }
        viewModelScope.launch {
            val result = changeAiFeature(pid, before, feature, enabled)
            val saved = result.getOrNull()
            if (saved != null) {
                _state.update { it.copy(savingFeature = null, preferences = saved) }
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update { it.copy(savingFeature = null, preferences = before) }
            if (error is DomainError.Conflict && error.code == ChangeAiFeatureUseCase.CODE_CONSENT_REQUIRED) {
                // El consentimiento se retiró en otro lado: se ofrece activarlo otra vez.
                _state.update { it.copy(preferences = before.copy(consentGranted = false), consentOffer = ConsentOffer(feature)) }
            } else {
                _events.send(AiFeaturesEvent.ShowSaveFailed(offline = error == DomainError.Network))
            }
        }
    }

    private suspend fun refresh() {
        val pid = patientId ?: return
        val preferences = getAiPreferences(pid)
        _state.update { if (it.savingFeature == null) it.copy(isLoading = false, preferences = preferences) else it }
    }
}
