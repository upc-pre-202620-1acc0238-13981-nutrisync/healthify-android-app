package pe.edu.upc.healthify.features.main.presentation.screen

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
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
import pe.edu.upc.healthify.features.main.presentation.state.PractitionerSettingsUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PractitionerSettingsEvent
import pe.edu.upc.healthify.features.main.presentation.viewmodel.PractitionerSettingsViewModel

/** Destinos de PR20 (pantallas del grafo raíz). */
data class PractitionerSettingsCallbacks(
    /** «Atrás»: vuelve a la pestaña donde estaba el nutricionista (Ajustes se abre desde el menú principal). */
    val onBack: () -> Unit,
    /** PR20.1 · confirmación de cierre de sesión. */
    val onSignOut: () -> Unit,
)

data class PractitionerSettingsActions(
    val callbacks: PractitionerSettingsCallbacks,
    val onOpenLanguage: () -> Unit = {},
    val onDismissLanguage: () -> Unit = {},
    val onLanguageSelected: (PreferredLanguage) -> Unit = {},
)

/** PR20 · Ajustes del nutricionista (+ PR20.I idioma), pantalla interna que se abre desde el menú principal. */
@Composable
fun PractitionerSettingsScreen(
    callbacks: PractitionerSettingsCallbacks,
    modifier: Modifier = Modifier,
    viewModel: PractitionerSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            // Desde Android 13 el sistema (LocaleManager) recrea la pantalla solo; antes, se recrea aquí.
            PractitionerSettingsEvent.LanguageApplied ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) activity?.recreate()
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    PractitionerSettingsContent(
        state = state,
        actions = PractitionerSettingsActions(
            callbacks = callbacks,
            onOpenLanguage = viewModel::onOpenLanguage,
            onDismissLanguage = viewModel::onDismissLanguage,
            onLanguageSelected = viewModel::onLanguageSelected,
        ),
        modifier = modifier,
    )
}

@Composable
fun PractitionerSettingsContent(
    state: PractitionerSettingsUiState,
    actions: PractitionerSettingsActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val callbacks = actions.callbacks
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.settings_title), onBack = callbacks.onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space24),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            // DECISIÓN PR20: como PT21, «Mi cuenta» no tiene pantalla en el Figma; se muestra sin chevron ni destino.
            ListCard(
                title = stringResource(R.string.settings_account),
                supportingText = listOf(state.fullName, state.email).filter(String::isNotEmpty).joinToString(" · "),
                leadingIcon = HealthifyIcons.Person,
            )
            ListCard(
                title = stringResource(R.string.settings_language),
                supportingText = stringResource(state.language.labelRes),
                leadingIcon = HealthifyIcons.Language,
                onClick = actions.onOpenLanguage,
            )
            // PR20.1: cerrar sesión funciona igual sin conexión (el cierre es local).
            ListCard(
                title = stringResource(R.string.sign_out_dialog_confirm),
                leadingIcon = HealthifyIcons.Logout,
                onClick = callbacks.onSignOut,
            )
        }
    }
    if (state.showLanguageSheet) {
        HealthifyBottomSheet(onDismissRequest = actions.onDismissLanguage) {
            LanguageSheetContent(selected = state.language, onSelect = actions.onLanguageSelected, onClose = actions.onDismissLanguage)
        }
    }
}

private val PreviewCallbacks = PractitionerSettingsCallbacks({}, {})

private val PreviewState = PractitionerSettingsUiState(
    fullName = "Lucía Paredes",
    email = "lucia.paredes@correo.com",
)

@Preview(name = "PR20 · Ajustes", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerSettingsPreview() {
    HealthifyTheme {
        PractitionerSettingsContent(state = PreviewState, actions = PractitionerSettingsActions(PreviewCallbacks))
    }
}

@Preview(name = "PR20 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerSettingsOfflinePreview() {
    HealthifyTheme {
        PractitionerSettingsContent(
            state = PreviewState.copy(isOffline = true),
            actions = PractitionerSettingsActions(PreviewCallbacks),
        )
    }
}

@Preview(name = "PR20.I · Idioma", widthDp = 360, heightDp = 400)
@Composable
private fun PractitionerLanguageSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            LanguageSheetContent(selected = PreferredLanguage.SPANISH, onSelect = {}, onClose = {})
        }
    }
}
