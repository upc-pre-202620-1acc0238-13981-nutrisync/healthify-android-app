package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PendingConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PendingConsentUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.PendingConsentViewModel

/** PT2.1 · Vínculo pendiente de consentimiento: sin consentimiento la app no muestra datos clínicos. */
@Composable
fun PendingConsentScreen(
    onReviewConsent: (careLinkId: Long) -> Unit,
    onScanInvitation: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingConsentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is PendingConsentEvent.NavigateToConsent -> onReviewConsent(event.careLinkId)
            PendingConsentEvent.NavigateToScanInvitation -> onScanInvitation()
            PendingConsentEvent.SignedOut -> onSignedOut()
        }
    }
    PendingConsentContent(
        state = state,
        onReviewConsent = viewModel::onReviewConsent,
        onSignOut = viewModel::onSignOut,
        modifier = modifier,
    )
}

@Composable
fun PendingConsentContent(
    state: PendingConsentUiState,
    onReviewConsent: () -> Unit,
    onSignOut: () -> Unit,
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
        HealthifyTopAppBar(title = stringResource(R.string.pending_consent_title))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space4, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.pending_consent_heading),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.pending_consent_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = stringResource(R.string.pending_consent_review),
                onClick = onReviewConsent,
                enabled = !state.isSigningOut,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.sign_out_dialog_confirm),
                onClick = onSignOut,
                style = HealthifyButtonStyle.Text,
                loading = state.isSigningOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "PT2.1 · Vínculo pendiente de consentimiento", widthDp = 360, heightDp = 800)
@Composable
private fun PendingConsentPreview() {
    HealthifyTheme { PendingConsentContent(state = PendingConsentUiState(), onReviewConsent = {}, onSignOut = {}) }
}

@Preview(name = "PT2.1 · Cerrando sesión", widthDp = 360, heightDp = 800)
@Composable
private fun PendingConsentSigningOutPreview() {
    HealthifyTheme {
        PendingConsentContent(state = PendingConsentUiState(isSigningOut = true), onReviewConsent = {}, onSignOut = {})
    }
}
