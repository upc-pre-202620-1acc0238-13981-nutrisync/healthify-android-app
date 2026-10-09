package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import pe.edu.upc.healthify.features.carerelationship.presentation.components.InvitationQrCode
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InviteError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.InvitePatientUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.InvitePatientViewModel
import java.time.Duration

/** PR2 · Generar invitación (+ PR2.L). ‹ vuelve a PR1. */
@Composable
fun InvitePatientScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitePatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    InvitePatientContent(
        state = state,
        onBack = onBack,
        onValidityChange = viewModel::onValidityChange,
        onGenerateAnother = viewModel::onGenerateAnother,
        modifier = modifier,
    )
}

@Composable
fun InvitePatientContent(
    state: InvitePatientUiState,
    onBack: () -> Unit,
    onValidityChange: (InvitationValidity) -> Unit,
    onGenerateAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.invite_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            val token = state.token
            when {
                state.isGenerating && token == null -> {
                    SkeletonCard(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.invite_generating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                token == null -> ErrorState(
                    onRetry = onGenerateAnother,
                    title = stringResource(R.string.ds_error_state_title),
                    text = stringResource(state.error.messageRes()),
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> InvitationBody(state, token, onValidityChange, onGenerateAnother)
            }
        }
    }
}

@Composable
private fun InvitationBody(
    state: InvitePatientUiState,
    token: InvitationToken,
    onValidityChange: (InvitationValidity) -> Unit,
    onGenerateAnother: () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.invite_instructions),
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurfaceVariant,
    )
    HealthifyCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space16),
        ) {
            Text(
                text = stringResource(R.string.invite_expires_in_label),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
            ValiditySelector(
                selected = state.validity,
                enabled = !state.isGenerating && !state.isOffline,
                onSelect = onValidityChange,
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                InvitationQrCode(
                    content = token.value,
                    contentDescription = stringResource(R.string.invite_qr_cd),
                )
            }
            // DECISIÓN PR2: «Válido 24h · un solo uso» del frame pasa a ser la cuenta regresiva de la vigencia.
            Text(
                text = if (state.isExpired) {
                    stringResource(R.string.invite_expired)
                } else {
                    stringResource(R.string.invite_countdown, state.remaining.countdownText())
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isExpired) scheme.error else scheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
        HealthifyButton(
            text = stringResource(R.string.invite_generate_another),
            onClick = onGenerateAnother,
            style = HealthifyButtonStyle.Text,
            enabled = !state.isOffline,
            loading = state.isGenerating,
        )
        Text(
            text = stringResource(R.string.invite_previous_still_valid),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        if (state.error != null) {
            Text(
                text = stringResource(state.error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
            )
        }
    }
}

/** Campo «Vence en» (52 dp del frame) que abre las vigencias disponibles. */
@Composable
private fun ValiditySelector(
    selected: InvitationValidity,
    enabled: Boolean,
    onSelect: (InvitationValidity) -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.touchMin)
                .clip(MaterialTheme.shapes.medium)
                .background(scheme.surfaceContainer)
                .border(dimens.borderThin, scheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(enabled = enabled, role = Role.DropdownList) { expanded = true }
                .padding(horizontal = dimens.space16, vertical = dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(selected.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = HealthifyIcons.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.rotate(QUARTER_TURN),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = scheme.surfaceContainerLowest,
        ) {
            InvitationValidity.entries.forEach { validity ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(validity.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(validity)
                    },
                )
            }
        }
    }
}

private const val QUARTER_TURN = 90f

private fun InvitationValidity.labelRes(): Int = when (this) {
    InvitationValidity.ONE_HOUR -> R.string.invite_validity_one_hour
    InvitationValidity.ONE_DAY -> R.string.invite_validity_one_day
    InvitationValidity.SEVEN_DAYS -> R.string.invite_validity_seven_days
}

private fun InviteError?.messageRes(): Int = when (this) {
    InviteError.OFFLINE -> R.string.invite_error_offline
    InviteError.EXPIRATION_REQUIRED -> R.string.invite_error_expiration_required
    InviteError.GENERIC, null -> R.string.ds_error_state_body
}

/** «23:59:41» o, con más de un día, «6 d 23:59:41». */
@Composable
private fun Duration?.countdownText(): String {
    val total = (this ?: Duration.ZERO).seconds.coerceAtLeast(0)
    val days = total / SECONDS_PER_DAY
    val clock = stringResource(
        R.string.invite_countdown_clock,
        (total % SECONDS_PER_DAY) / SECONDS_PER_HOUR,
        (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE,
        total % SECONDS_PER_MINUTE,
    )
    return if (days > 0) stringResource(R.string.invite_countdown_days, days, clock) else clock
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L

private val previewToken = InvitationToken("Q2hhbmdlTWVQbGVhc2VUb2tlbjEyMzQ1Njc4OTA")

@Preview(name = "PR2 · Generar invitación", widthDp = 360, heightDp = 800)
@Composable
private fun InvitePatientPreview() {
    HealthifyTheme {
        InvitePatientContent(
            state = InvitePatientUiState(
                isGenerating = false,
                token = previewToken,
                remaining = Duration.ofHours(23).plusMinutes(59).plusSeconds(41),
            ),
            onBack = {},
            onValidityChange = {},
            onGenerateAnother = {},
        )
    }
}

@Preview(name = "PR2.L · Generando código", widthDp = 360, heightDp = 800)
@Composable
private fun InvitePatientLoadingPreview() {
    HealthifyTheme {
        InvitePatientContent(state = InvitePatientUiState(), onBack = {}, onValidityChange = {}, onGenerateAnother = {})
    }
}

@Preview(name = "PR2 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun InvitePatientOfflinePreview() {
    HealthifyTheme {
        InvitePatientContent(
            state = InvitePatientUiState(isGenerating = false, isOffline = true, error = InviteError.OFFLINE),
            onBack = {},
            onValidityChange = {},
            onGenerateAnother = {},
        )
    }
}
