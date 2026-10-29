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

/** Practitioner settings destinations (root graph screens). */
data class PractitionerSettingsCallbacks(
    /** "Back": returns to the tab the practitioner was on (Settings opens from the main menu). */
    val onBack: () -> Unit,
    /** Sign-out confirmation. */
    val onSignOut: () -> Unit,
)

data class PractitionerSettingsActions(
    val callbacks: PractitionerSettingsCallbacks,
    val onOpenLanguage: () -> Unit = {},
    val onDismissLanguage: () -> Unit = {},
    val onLanguageSelected: (PreferredLanguage) -> Unit = {},
)

/** Practitioner settings (+ language), an inner screen opened from the main menu. */
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
            // Since Android 13 the system (LocaleManager) recreates the screen itself; before that, it is recreated here.
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
            // As in patient settings, "My account" has no screen in the Figma; it shows without a chevron or destination.
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
            // Sign out works the same offline (it is local).
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

@Preview(name = "Practitioner settings", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerSettingsPreview() {
    HealthifyTheme {
        PractitionerSettingsContent(state = PreviewState, actions = PractitionerSettingsActions(PreviewCallbacks))
    }
}

@Preview(name = "Practitioner settings - offline", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerSettingsOfflinePreview() {
    HealthifyTheme {
        PractitionerSettingsContent(
            state = PreviewState.copy(isOffline = true),
            actions = PractitionerSettingsActions(PreviewCallbacks),
        )
    }
}

@Preview(name = "Practitioner settings - language", widthDp = 360, heightDp = 400)
@Composable
private fun PractitionerLanguageSheetPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            LanguageSheetContent(selected = PreferredLanguage.SPANISH, onSelect = {}, onClose = {})
        }
    }
}
