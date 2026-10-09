package pe.edu.upc.healthify.features.main.presentation.screen

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ListCard
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.main.presentation.components.LanguageSheetContent
import pe.edu.upc.healthify.features.main.presentation.components.labelRes
import pe.edu.upc.healthify.features.main.presentation.state.PatientSettingsUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PatientSettingsEvent
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PatientSettingsViewModel

/** Destinos de PT21 (pantallas del grafo raíz sobre el shell). */
data class PatientSettingsCallbacks(
    val onOpenReminders: () -> Unit,
    val onOpenAiFeatures: () -> Unit,
    val onOpenPendingSync: () -> Unit,
    val onWithdrawConsent: () -> Unit,
    val onSwitchPractitioner: () -> Unit,
    /** PT21.1 · confirmación de cierre de sesión. */
    val onSignOut: () -> Unit,
)

data class PatientSettingsActions(
    val callbacks: PatientSettingsCallbacks,
    val onOpenLanguage: () -> Unit = {},
    val onDismissLanguage: () -> Unit = {},
    val onLanguageSelected: (PreferredLanguage) -> Unit = {},
    val onDataExport: () -> Unit = {},
    val onDismissDataExport: () -> Unit = {},
)

/** PT21 · Ajustes del paciente (+ PT21.I idioma), pestaña «Ajustes» del shell. */
@Composable
fun PatientSettingsScreen(
    callbacks: PatientSettingsCallbacks,
    modifier: Modifier = Modifier,
    viewModel: PatientSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            // Desde Android 13 el sistema (LocaleManager) recrea la pantalla solo; antes, se recrea aquí.
            PatientSettingsEvent.LanguageApplied ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) activity?.recreate()
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PatientSettingsContent(
        state = state,
        actions = PatientSettingsActions(
            callbacks = callbacks,
            onOpenLanguage = viewModel::onOpenLanguage,
            onDismissLanguage = viewModel::onDismissLanguage,
            onLanguageSelected = viewModel::onLanguageSelected,
            onDataExport = viewModel::onDataExport,
            onDismissDataExport = viewModel::onDismissDataExport,
        ),
        modifier = modifier,
    )
}

@Composable
fun PatientSettingsContent(
    state: PatientSettingsUiState,
    actions: PatientSettingsActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val callbacks = actions.callbacks
    // Sin conexión: los ítems que necesitan red se ven deshabilitados (nota de PT21).
    val online = !state.isOffline
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.settings_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            // DECISIÓN PT21: «Mi cuenta» no tiene pantalla en el Figma; se muestra sin chevron ni destino.
            ListCard(
                title = stringResource(R.string.settings_account),
                supportingText = listOf(state.fullName, state.email).filter(String::isNotEmpty).joinToString(" · "),
                leadingIcon = HealthifyIcons.Person,
            )
            ListCard(
                title = stringResource(R.string.settings_reminders),
                leadingIcon = HealthifyIcons.Bell,
                onClick = callbacks.onOpenReminders,
            )
            ListCard(
                title = stringResource(R.string.settings_language),
                supportingText = stringResource(state.language.labelRes),
                leadingIcon = HealthifyIcons.Language,
                onClick = actions.onOpenLanguage,
            )
            SettingsItem(
                title = stringResource(R.string.settings_ai_features),
                supportingText = state.aiFeaturesActive?.let {
                    stringResource(if (it) R.string.settings_ai_features_on else R.string.settings_ai_features_off)
                },
                leadingIcon = HealthifyIcons.Ai,
                enabled = online,
                onClick = callbacks.onOpenAiFeatures,
            )
            ListCard(
                title = stringResource(R.string.settings_pending_sync),
                leadingIcon = HealthifyIcons.Sync,
                onClick = callbacks.onOpenPendingSync,
            )
            SettingsItem(
                title = stringResource(R.string.settings_withdraw_consent),
                leadingIcon = HealthifyIcons.Logout,
                enabled = online,
                onClick = callbacks.onWithdrawConsent,
            )
            SettingsItem(
                title = stringResource(R.string.settings_switch_practitioner),
                supportingText = stringResource(R.string.settings_switch_practitioner_support),
                leadingIcon = HealthifyIcons.Qr,
                enabled = online,
                onClick = callbacks.onSwitchPractitioner,
            )
            SettingsItem(
                title = stringResource(R.string.settings_data_export),
                supportingText = stringResource(R.string.settings_data_export_support),
                leadingIcon = HealthifyIcons.Download,
                enabled = online,
                onClick = actions.onDataExport,
            )
            // PT21.1: cerrar sesión funciona igual sin conexión (los pendientes se conservan).
            ListCard(
                title = stringResource(R.string.sign_out_dialog_confirm),
                leadingIcon = HealthifyIcons.Logout,
                onClick = callbacks.onSignOut,
            )
        }
    }
    if (state.showLanguageSheet) {
        HealthifyBottomSheet(onDismissRequest = actions.onDismissLanguage) {
            LanguageSheetContent(
                selected = state.language,
                onSelect = actions.onLanguageSelected,
                onClose = actions.onDismissLanguage,
            )
        }
    }
    if (state.showDataExportDialog) {
        HealthifyDialog(
            title = stringResource(R.string.settings_data_export_dialog_title),
            text = stringResource(R.string.settings_data_export_dialog_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onDismissDataExport,
            onDismiss = actions.onDismissDataExport,
            dismissLabel = null,
        )
    }
}

/** Ítem que necesita conexión: sin red se ve deshabilitado y no responde. */
@Composable
private fun SettingsItem(
    title: String,
    leadingIcon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    ListCard(
        title = title,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        onClick = if (enabled) onClick else null,
        showChevron = true,
        modifier = if (enabled) Modifier else Modifier.alpha(DISABLED_ALPHA),
    )
}

private const val DISABLED_ALPHA = 0.38f

private val PreviewCallbacks = PatientSettingsCallbacks({}, {}, {}, {}, {}, {})

private val PreviewState = PatientSettingsUiState(
    fullName = "María Flores",
    email = "maria.flores@correo.com",
    aiFeaturesActive = true,
)

@Preview(name = "PT21 · Ajustes", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSettingsPreview() {
    HealthifyTheme { PatientSettingsContent(state = PreviewState, actions = PatientSettingsActions(PreviewCallbacks)) }
}

@Preview(name = "PT21 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSettingsOfflinePreview() {
    HealthifyTheme {
        PatientSettingsContent(state = PreviewState.copy(isOffline = true), actions = PatientSettingsActions(PreviewCallbacks))
    }
}

@Preview(name = "PT21 · Copia de mis datos (Próximamente)", widthDp = 360, heightDp = 800)
@Composable
private fun PatientSettingsDataExportPreview() {
    HealthifyTheme {
        PatientSettingsContent(
            state = PreviewState.copy(showDataExportDialog = true),
            actions = PatientSettingsActions(PreviewCallbacks),
        )
    }
}

@Preview(name = "PT21.I · Idioma", widthDp = 360, heightDp = 400)
@Composable
private fun LanguageSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            LanguageSheetContent(selected = PreferredLanguage.SPANISH, onSelect = {}, onClose = {})
        }
    }
}
