package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.RedeemInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ScanInvitationRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.CameraPermissionState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationUiState
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.SignOutUseCase
import javax.inject.Inject

/**
 * PT1 · Escanear invitación. Compone Iam (paciente con sesión, cerrar sesión) y CareRelationship (canje, F5).
 *
 * La cámara entrega el mismo QR muchas veces por segundo: se canjea uno a la vez y un código ya rechazado no se
 * vuelve a enviar hasta leer otro distinto (el token es de un solo uso y cada intento cuenta en el servidor).
 */
@HiltViewModel
class ScanInvitationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val redeemInvitation: RedeemInvitationUseCase,
    private val signOut: SignOutUseCase,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ScanInvitationUiState(
            replaceActiveLink = savedStateHandle.get<Boolean>(ScanInvitationRoute.ARG_REPLACE_ACTIVE_LINK) ?: false,
        ),
    )
    val state: StateFlow<ScanInvitationUiState> = _state.asStateFlow()

    private val _events = Channel<ScanInvitationEvent>(Channel.BUFFERED)
    val events: Flow<ScanInvitationEvent> = _events.receiveAsFlow()

    /** Último contenido rechazado. Nunca va al UiState: puede ser un token (secreto). */
    private var lastRejectedContent: String? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onCameraPermissionResult(granted: Boolean) = _state.update {
        it.copy(cameraPermission = if (granted) CameraPermissionState.Granted else CameraPermissionState.Denied)
    }

    fun onQrScanned(content: String) {
        val current = _state.value
        if (!current.isScanning || content == lastRejectedContent) return
        _state.update { it.copy(isRedeeming = true, error = null) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                // La sesión terminó mientras se leía: el grafo raíz ya lleva a S6/S2.
                _state.update { it.copy(isRedeeming = false) }
                return@launch
            }
            val result = redeemInvitation(user.id.value, content, current.replaceActiveLink)
            val link = result.getOrNull()
            if (link != null) {
                lastRejectedContent = null
                _state.update { it.copy(isRedeeming = false) }
                _events.send(ScanInvitationEvent.NavigateToConsent(link.id.value))
            } else {
                val error = result.domainErrorOrNull().toScanInvitationError()
                // Sin red o con un error del servidor el mismo código puede volver a intentarse.
                lastRejectedContent = content.takeUnless { error.isRetryable }
                _state.update { it.copy(isRedeeming = false, error = error) }
            }
        }
    }

    fun onNoCameraClick() = _state.update { it.copy(showNoCameraDialog = true) }

    fun onNoCameraDismiss() = _state.update { it.copy(showNoCameraDialog = false) }

    /** «Atrás» cuando PT1 es la primera pantalla: PT21.1 «¿Cerrar sesión?». */
    fun onBackAtRoot() = _state.update { it.copy(showSignOutDialog = true) }

    fun onSignOutDismiss() = _state.update { it.copy(showSignOutDialog = false) }

    fun onSignOutConfirm() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            signOut()
            _state.update { it.copy(isSigningOut = false, showSignOutDialog = false) }
            _events.send(ScanInvitationEvent.SignedOut)
        }
    }

    private val ScanInvitationError.isRetryable: Boolean
        get() = this == ScanInvitationError.Network || this == ScanInvitationError.Unexpected
}

/** Validaciones de la nota de PT1 por `extensions.code` (X-3); sin código decide el status. */
internal fun DomainError?.toScanInvitationError(): ScanInvitationError = when (this) {
    // `InvitationNotValid` (también el rechazo local del formato) y el 400 de model binding: «no es válido».
    is DomainError.Validation ->
        if (code == CODE_INVITATION_EXPIRED) ScanInvitationError.Expired else ScanInvitationError.InvalidFormat
    is DomainError.NotFound -> ScanInvitationError.NotFound
    is DomainError.Conflict -> when (code) {
        CODE_ALREADY_REDEEMED -> ScanInvitationError.AlreadyUsed
        CODE_ALREADY_HAS_ACTIVE_LINK -> ScanInvitationError.AlreadyLinked
        CODE_ALREADY_LINKED_TO_PRACTITIONER -> ScanInvitationError.SamePractitioner
        else -> ScanInvitationError.Unexpected
    }
    is DomainError.Forbidden -> if (code == CODE_SELF_LINK) ScanInvitationError.SelfLink else ScanInvitationError.Unexpected
    DomainError.Network -> ScanInvitationError.Network
    is DomainError.Unauthorized, is DomainError.Unexpected, null -> ScanInvitationError.Unexpected
}

private const val CODE_INVITATION_EXPIRED = "InvitationExpired"
private const val CODE_ALREADY_REDEEMED = "InvitationAlreadyRedeemed"
private const val CODE_ALREADY_HAS_ACTIVE_LINK = "PatientAlreadyHasActiveLink"
private const val CODE_ALREADY_LINKED_TO_PRACTITIONER = "AlreadyLinkedToThisPractitioner"
private const val CODE_SELF_LINK = "PatientCannotSelfLink"
