package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.components.QrCodeScanner
import pe.edu.upc.healthify.core.designsystem.component.ViewfinderCorners
import pe.edu.upc.healthify.features.carerelationship.presentation.state.CameraPermissionState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.ScanInvitationViewModel

// Medidas del frame «PT1 · Escanear invitación».
private val ViewfinderSize = 296.dp
private val ViewfinderTopSpacing = 132.dp
private val ProgressSize = 16.dp
private val ProgressStroke = 2.dp

/** Acciones de PT1 que la pantalla con estado conecta con el ViewModel y el sistema. */
data class ScanInvitationActions(
    val onBack: () -> Unit = {},
    val onNoCamera: () -> Unit = {},
    val onNoCameraDismiss: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onSignOutConfirm: () -> Unit = {},
    val onSignOutDismiss: () -> Unit = {},
)

/**
 * PT1 · Escanear invitación (+ PT1.M permiso de cámara).
 *
 * @param onBack `null` cuando PT1 es la primera pantalla (desde S5 o PT24): «atrás» ofrece cerrar sesión.
 * @param onExitApp «Salir» de PT1.M sin pantalla anterior.
 */
@Composable
fun ScanInvitationScreen(
    onConsentRequired: (careLinkId: Long) -> Unit,
    onSignedOut: () -> Unit,
    onBack: (() -> Unit)?,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanInvitationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is ScanInvitationEvent.NavigateToConsent -> onConsentRequired(event.careLinkId)
            ScanInvitationEvent.SignedOut -> onSignedOut()
        }
    }

    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onCameraPermissionResult(it)
    }
    // Al volver de los ajustes del teléfono (PT1.M → «Abrir ajustes») se revisa otra vez el permiso.
    LifecycleResumeEffect(Unit) {
        if (context.hasCameraPermission()) {
            viewModel.onCameraPermissionResult(true)
        } else if (!permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onPauseOrDispose {}
    }

    val back = onBack ?: viewModel::onBackAtRoot
    BackHandler(enabled = onBack == null, onBack = viewModel::onBackAtRoot)

    ScanInvitationContent(
        state = state,
        actions = ScanInvitationActions(
            onBack = back,
            onNoCamera = viewModel::onNoCameraClick,
            onNoCameraDismiss = viewModel::onNoCameraDismiss,
            onOpenSettings = { context.openAppSettings() },
            onExit = onBack ?: onExitApp,
            onSignOutConfirm = viewModel::onSignOutConfirm,
            onSignOutDismiss = viewModel::onSignOutDismiss,
        ),
        modifier = modifier,
        cameraPreview = {
            QrCodeScanner(
                enabled = state.isScanning && !state.showNoCameraDialog && !state.showSignOutDialog,
                onQrCode = viewModel::onQrScanned,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/** @param cameraPreview vista de cámara (solo con permiso); las previews no la usan. */
@Composable
fun ScanInvitationContent(
    state: ScanInvitationUiState,
    actions: ScanInvitationActions,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit = {},
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(
            title = stringResource(R.string.scan_invitation_title),
            onBack = actions.onBack,
            containerColor = scheme.inverseSurface,
            contentColor = scheme.inverseOnSurface,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isOffline) {
                OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                Spacer(Modifier.height(dimens.space16))
            }
            Text(
                text = stringResource(R.string.scan_invitation_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.inversePrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(ViewfinderTopSpacing))
            Viewfinder(showCamera = state.cameraPermission == CameraPermissionState.Granted, cameraPreview = cameraPreview)
            Spacer(Modifier.height(dimens.space24))
            ScanStatus(state)
        }
        HealthifyButton(
            text = stringResource(R.string.scan_invitation_no_camera),
            onClick = actions.onNoCamera,
            style = HealthifyButtonStyle.Text,
            // Nota PT1 «Sin conexión: banner + botón inactivo».
            enabled = !state.isOffline && !state.isRedeeming,
            contentColor = scheme.inversePrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space24),
        )
    }

    ScanInvitationDialogs(state, actions)
}

@Composable
private fun Viewfinder(showCamera: Boolean, cameraPreview: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.scan_invitation_cd_viewfinder)
    Box(
        modifier = modifier
            .size(ViewfinderSize)
            .semantics { contentDescription = description },
    ) {
        if (showCamera) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large),
            ) { cameraPreview() }
        }
        ViewfinderCorners(color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxSize())
    }
}

/** Bajo el visor: la pista, «Buscando el código…» o el mensaje de la última lectura rechazada. */
@Composable
private fun ScanStatus(state: ScanInvitationUiState, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val error = state.error
    when {
        state.isRedeeming -> Row(
            modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = scheme.secondary,
                strokeWidth = ProgressStroke,
            )
            Text(
                text = stringResource(R.string.scan_invitation_searching),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.inverseOnSurface,
            )
        }
        error != null -> Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.onSurface)
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            Icon(
                imageVector = HealthifyIcons.Info,
                contentDescription = null,
                tint = scheme.secondary,
                modifier = Modifier.size(dimens.icon),
            )
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.inverseOnSurface,
            )
        }
        else -> Text(
            text = stringResource(R.string.scan_invitation_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.inverseOnSurface,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScanInvitationDialogs(state: ScanInvitationUiState, actions: ScanInvitationActions) {
    when {
        // PT1.M · permiso de cámara denegado.
        state.cameraPermission == CameraPermissionState.Denied -> HealthifyDialog(
            title = stringResource(R.string.camera_permission_title),
            text = stringResource(R.string.camera_permission_body),
            confirmLabel = stringResource(R.string.camera_permission_open_settings),
            onConfirm = actions.onOpenSettings,
            onDismiss = actions.onExit,
            dismissLabel = stringResource(R.string.camera_permission_exit),
        )
        state.showNoCameraDialog -> HealthifyDialog(
            title = stringResource(R.string.no_camera_dialog_title),
            text = stringResource(R.string.no_camera_dialog_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onNoCameraDismiss,
            onDismiss = actions.onNoCameraDismiss,
            dismissLabel = null,
        )
        state.showSignOutDialog -> HealthifyDialog(
            title = stringResource(R.string.sign_out_dialog_title),
            text = stringResource(R.string.sign_out_dialog_body),
            confirmLabel = stringResource(R.string.sign_out_dialog_confirm),
            onConfirm = actions.onSignOutConfirm,
            onDismiss = actions.onSignOutDismiss,
            confirmLoading = state.isSigningOut,
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Preview(name = "PT1 · Escanear invitación", widthDp = 360, heightDp = 800)
@Composable
private fun ScanInvitationPreview() {
    HealthifyTheme {
        ScanInvitationContent(
            state = ScanInvitationUiState(cameraPermission = CameraPermissionState.Granted),
            actions = ScanInvitationActions(),
        )
    }
}

@Preview(name = "PT1.M · Escanear — permiso de cámara", widthDp = 360, heightDp = 800)
@Composable
private fun ScanInvitationPermissionPreview() {
    HealthifyTheme {
        ScanInvitationContent(
            state = ScanInvitationUiState(cameraPermission = CameraPermissionState.Denied),
            actions = ScanInvitationActions(),
        )
    }
}

@Preview(name = "PT1 · Buscando el código (carga)", widthDp = 360, heightDp = 800)
@Composable
private fun ScanInvitationLoadingPreview() {
    HealthifyTheme {
        ScanInvitationContent(
            state = ScanInvitationUiState(cameraPermission = CameraPermissionState.Granted, isRedeeming = true),
            actions = ScanInvitationActions(),
        )
    }
}

@Preview(name = "PT1 · Código vencido (error)", widthDp = 360, heightDp = 800)
@Composable
private fun ScanInvitationErrorPreview() {
    HealthifyTheme {
        ScanInvitationContent(
            state = ScanInvitationUiState(
                cameraPermission = CameraPermissionState.Granted,
                error = ScanInvitationError.Expired,
            ),
            actions = ScanInvitationActions(),
        )
    }
}

@Preview(name = "PT1 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun ScanInvitationOfflinePreview() {
    HealthifyTheme {
        ScanInvitationContent(
            state = ScanInvitationUiState(cameraPermission = CameraPermissionState.Granted, isOffline = true),
            actions = ScanInvitationActions(),
        )
    }
}
