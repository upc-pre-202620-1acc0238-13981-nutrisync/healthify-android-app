package pe.edu.upc.healthify.features.intake.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg
import pe.edu.upc.healthify.features.intake.presentation.components.clockTimeText
import pe.edu.upc.healthify.features.intake.presentation.components.gramsText
import pe.edu.upc.healthify.features.intake.presentation.components.numberText
import pe.edu.upc.healthify.features.intake.presentation.components.pendingRejectionRes
import pe.edu.upc.healthify.features.intake.presentation.components.previewPending
import pe.edu.upc.healthify.features.intake.presentation.state.PendingSyncItem
import pe.edu.upc.healthify.features.intake.presentation.state.PendingSyncUiState
import pe.edu.upc.healthify.features.intake.presentation.viewmodel.PendingSyncViewModel
import java.time.OffsetDateTime

// «Punto indicador» de PT19 (8 dp, `secondary`).
private val IndicatorSize = 8.dp
private val SyncingIndicatorSize = 16.dp

/** PT19 · Pendientes por sincronizar (+ PT19.V). */
@Composable
fun PendingSyncScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingSyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PendingSyncContent(state = state, onBack = onBack, modifier = modifier)
}

@Composable
fun PendingSyncContent(state: PendingSyncUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.pending_title), onBack = onBack)
        when {
            state.isLoading -> Column(
                modifier = Modifier.padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) { repeat(2) { SkeletonListItem() } }
            state.isEmpty -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.space16),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.pending_empty_title),
                    text = stringResource(R.string.pending_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = dimens.space16,
                    top = dimens.space16,
                    end = dimens.space16,
                    bottom = dimens.space40,
                ),
                verticalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                if (state.isOffline) item(key = "offline") { OfflineBanner(type = OfflineBannerType.Queueable) }
                if (state.isSyncing) item(key = "syncing") { SyncingRow() }
                items(items = state.items, key = { it.key }) { item ->
                    when (item) {
                        is PendingSyncItem.Meal -> PendingRow(item.entry)
                        is PendingSyncItem.WeighIn -> PendingWeighInRow(item.weighIn)
                    }
                }
                item(key = "footer") {
                    Text(
                        text = stringResource(R.string.pending_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = dimens.space12),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncingRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = HealthifyTheme.dimens.borderThick,
            modifier = Modifier.size(SyncingIndicatorSize),
        )
        Text(
            text = stringResource(R.string.pending_syncing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «● Lomo saltado · 320 g / pendiente de enviar»: la hora local se muestra tal como se guardó. */
@Composable
private fun PendingRow(entry: PendingDiaryEntry) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(IndicatorSize)
                        .clip(CircleShape)
                        .background(if (entry.isRejected) scheme.outline else scheme.secondary),
                )
                Text(
                    text = stringResource(R.string.format_separator_dot, entry.foodName, gramsText(entry.grams, approximate = !entry.confirmed)),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = entry.localTimestamp.value.clockTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (entry.isRejected) stringResource(pendingRejectionRes(entry.rejectionCode)) else stringResource(R.string.pending_status),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** «● Autopesaje · 68,4 kg / pendiente de enviar»: el valor que escribió el paciente, con la hora tal como se guardó. */
@Composable
private fun PendingWeighInRow(weighIn: PendingSelfWeighIn) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier
                .padding(dimens.space16)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(IndicatorSize)
                        .clip(CircleShape)
                        .background(if (weighIn.isRejected) scheme.outline else scheme.secondary),
                )
                Text(
                    text = stringResource(
                        R.string.pending_weigh_in,
                        stringResource(R.string.format_kg, numberText(weighIn.weight.value)),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = weighIn.localTimestamp.value.clockTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (weighIn.isRejected) {
                    stringResource(weighInRejectionRes(weighIn.rejectionCode))
                } else {
                    stringResource(R.string.pending_status)
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

private fun weighInRejectionRes(code: String?): Int = when (code) {
    "ImplausibleWeightValue" -> R.string.pending_rejected_weight
    else -> pendingRejectionRes(code)
}

@Preview(name = "PT19 · Pendientes", widthDp = 360, heightDp = 800)
@Composable
private fun PendingSyncPreview() {
    HealthifyTheme {
        PendingSyncContent(
            state = PendingSyncUiState(
                isLoading = false,
                isOffline = true,
                entries = listOf(
                    previewPending("Lomo saltado", "13:15"),
                    previewPending("Ceviche", "14:10", rejection = "LocalTimestampCannotBeRewritten").copy(
                        clientEntryId = ClientEntryId("6f1d2c3b-4a59-4e8f-9b7a-0c1d2e3f4a5b"),
                    ),
                ),
                weighIns = listOf(
                    PendingSelfWeighIn(
                        clientEntryId = ClientEntryId("0b7e2a51-3c4d-4f6e-8a9b-1c2d3e4f5a6b"),
                        weight = WeightKg.of(68.4),
                        localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse("2026-09-09T07:30:00-05:00")),
                        fasted = true,
                    ),
                ),
            ),
            onBack = {},
        )
    }
}

@Preview(name = "PT19.V · Todo al día", widthDp = 360, heightDp = 800)
@Composable
private fun PendingSyncEmptyPreview() {
    HealthifyTheme { PendingSyncContent(state = PendingSyncUiState(isLoading = false), onBack = {}) }
}
