package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifySwitchRow
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.ConsentViewModel

// Medidas del frame «PT2 · Consentimiento y alcance».
private val BadgeSize = 40.dp
private val BadgeIconSize = 18.dp
private val BulletSize = 8.dp
private val BulletTopOffset = 6.dp

/** Acciones de PT2 que la pantalla con estado conecta con el ViewModel. */
data class ConsentActions(
    val onBack: () -> Unit = {},
    val onAiProcessingChange: (Boolean) -> Unit = {},
    val onGrantConsent: () -> Unit = {},
    val onNotNow: () -> Unit = {},
    val onErrorAction: () -> Unit = {},
    val onErrorDismiss: () -> Unit = {},
    val onServerErrorRetry: () -> Unit = {},
    val onServerErrorDismiss: () -> Unit = {},
)

/** PT2 · Consentimiento y alcance (con la sección «Inteligencia artificial», CR-2). */
@Composable
fun ConsentScreen(
    onBack: () -> Unit,
    onConsentGranted: () -> Unit,
    onNotNow: () -> Unit,
    onScanInvitation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConsentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Recién canjeado, PT2 es la primera pantalla: atrás deja el vínculo pendiente (PT2.1) en vez de cerrar la app.
    BackHandler(onBack = onBack)
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ConsentEvent.NavigateToHome -> onConsentGranted()
            ConsentEvent.NavigateToPendingConsent -> onNotNow()
            ConsentEvent.NavigateToScanInvitation -> onScanInvitation()
        }
    }
    ConsentContent(
        state = state,
        actions = ConsentActions(
            onBack = onBack,
            onAiProcessingChange = viewModel::onAiProcessingChange,
            onGrantConsent = viewModel::onGrantConsent,
            onNotNow = viewModel::onNotNow,
            onErrorAction = viewModel::onErrorAction,
            onErrorDismiss = viewModel::onErrorDismiss,
            onServerErrorRetry = viewModel::onServerErrorRetry,
            onServerErrorDismiss = viewModel::onServerErrorDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun ConsentContent(
    state: ConsentUiState,
    actions: ConsentActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.consent_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            ShieldBadge()
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
                Text(
                    text = stringResource(R.string.consent_intro),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                ConsentTermsCard(
                    aiProcessingGranted = state.aiProcessingGranted,
                    onAiProcessingChange = actions.onAiProcessingChange,
                    switchEnabled = !state.isSubmitting,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = stringResource(if (state.isSubmitting) R.string.consent_granting else R.string.consent_grant),
                onClick = actions.onGrantConsent,
                loading = state.isSubmitting,
                // Nota PT2 «Sin conexión: botón deshabilitado».
                enabled = !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.consent_not_now),
                onClick = actions.onNotNow,
                style = HealthifyButtonStyle.Text,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.error?.let { error -> ConsentErrorDialog(error, actions) }
    if (state.showServerError) {
        ServerErrorDialog(onRetry = actions.onServerErrorRetry, onDismiss = actions.onServerErrorDismiss)
    }
}

@Composable
private fun ShieldBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(BadgeSize)
            .clip(CircleShape)
            .background(scheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = HealthifyIcons.Shield,
            contentDescription = null,
            tint = scheme.surfaceContainerLowest,
            modifier = Modifier.size(BadgeIconSize),
        )
    }
}

/** «Card · Qué compartes y tus derechos»: el texto que el paciente acepta (versión de `ConsentScope.CURRENT`). */
@Composable
private fun ConsentTermsCard(
    aiProcessingGranted: Boolean,
    onAiProcessingChange: (Boolean) -> Unit,
    switchEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val shape = RoundedCornerShape(dimens.radiusXl)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .elevation(dimens.elevation1, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(start = dimens.space32, top = dimens.space32, end = dimens.space16, bottom = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) {
        Section(R.string.consent_section_share, R.string.consent_share_diary, R.string.consent_share_photos)
        Section(
            R.string.consent_section_practitioner,
            R.string.consent_practitioner_read,
            R.string.consent_practitioner_adjust,
            R.string.consent_practitioner_not_replacement,
        )
        Section(R.string.consent_section_ai, R.string.consent_ai_what, R.string.consent_ai_limits)
        HealthifySwitchRow(
            label = stringResource(R.string.consent_ai_switch),
            checked = aiProcessingGranted,
            onCheckedChange = onAiProcessingChange,
            enabled = switchEnabled,
        )
        Section(
            R.string.consent_section_rights,
            R.string.consent_rights_withdraw,
            R.string.consent_rights_effect,
            R.string.consent_rights_copy,
        )
        Section(R.string.consent_section_privacy, R.string.consent_privacy_encrypted, R.string.consent_privacy_law)
    }
}

@Composable
private fun ColumnScope.Section(@StringRes title: Int, @StringRes vararg items: Int) {
    Text(
        text = stringResource(title),
        style = HealthifyTheme.extendedTypography.overlineSection,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
    items.forEach { Bullet(stringResource(it)) }
}

@Composable
private fun Bullet(text: String, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Box(
            modifier = Modifier
                .padding(top = BulletTopOffset)
                .size(BulletSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConsentErrorDialog(error: ConsentError, actions: ConsentActions) {
    HealthifyDialog(
        title = stringResource(error.titleRes),
        text = stringResource(error.messageRes),
        confirmLabel = stringResource(error.actionRes),
        onConfirm = actions.onErrorAction,
        onDismiss = actions.onErrorDismiss,
        dismissLabel = null,
    )
}

@Preview(name = "PT2 · Consentimiento y alcance", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentPreview() {
    HealthifyTheme { ConsentContent(state = ConsentUiState(), actions = ConsentActions()) }
}

@Preview(name = "PT2 · Con IA aceptada", widthDp = 360, heightDp = 1600)
@Composable
private fun ConsentAiPreview() {
    HealthifyTheme { ConsentContent(state = ConsentUiState(aiProcessingGranted = true), actions = ConsentActions()) }
}

@Preview(name = "PT2 · Confirmando… (carga)", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentLoadingPreview() {
    HealthifyTheme { ConsentContent(state = ConsentUiState(isSubmitting = true), actions = ConsentActions()) }
}

@Preview(name = "PT2 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentOfflinePreview() {
    HealthifyTheme { ConsentContent(state = ConsentUiState(isOffline = true), actions = ConsentActions()) }
}

@Preview(name = "PT2 · Vínculo dado de alta (error)", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentErrorPreview() {
    HealthifyTheme {
        ConsentContent(state = ConsentUiState(error = ConsentError.LinkDischarged), actions = ConsentActions())
    }
}
