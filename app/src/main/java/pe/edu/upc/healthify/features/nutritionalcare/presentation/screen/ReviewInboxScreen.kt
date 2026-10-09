package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.InboxRow
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewInboxUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewInboxViewModel
import java.time.Instant

// Punto de «sin revisar» de cada fila (frame PR13).
private val UnreadDotSize = 8.dp

/**
 * PR13 · Bandeja de revisión (pestaña «Bandeja» del shell del nutricionista) + PR13.V al día. Nada de lo que hay aquí
 * cambió un plan: tocar un ítem abre PR14 (o PR14.IA si la IA propuso un plan).
 */
@Composable
fun ReviewInboxScreen(
    onOpenItem: (reviewItemId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    ReviewInboxContent(state = state, onOpenItem = onOpenItem, onRetry = viewModel::onRetry, modifier = modifier)
}

@Composable
fun ReviewInboxContent(
    state: ReviewInboxUiState,
    onOpenItem: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.inbox_title))
        val items = state.items
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                items != null && items.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space12),
                ) {
                    if (state.isOffline) {
                        item(key = "offline") { OfflineBanner(type = OfflineBannerType.RequiresNetwork) }
                    }
                    // DECISIÓN PR13: el conteo de PR13.1 se muestra siempre que hay ítems (no solo tras resolver uno).
                    item(key = "count") {
                        Text(
                            text = pluralStringResource(R.plurals.inbox_pending_count, items.size, items.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(items, key = { it.id }) { row -> InboxCard(row = row, onClick = { onOpenItem(row.id) }) }
                }
                state.needsConnection -> CenteredColumn {
                    OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                    EmptyState(
                        title = stringResource(R.string.practitioner_offline_title),
                        text = stringResource(R.string.inbox_offline_body),
                        actionLabel = stringResource(R.string.ds_retry),
                        onAction = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.isLoading -> CenteredColumn {
                    repeat(SKELETON_ROWS) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
                }
                state.isEmpty -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(dimens.space12, Alignment.CenterVertically),
                ) {
                    if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
                    EmptyState(
                        title = stringResource(R.string.inbox_empty_title),
                        text = stringResource(R.string.inbox_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.loadFailed -> CenteredColumn { ErrorState(onRetry = onRetry, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    val dimens = HealthifyTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.space16),
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
    ) { content() }
}

@Composable
private fun InboxCard(row: InboxRow, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val name = row.patientName ?: stringResource(R.string.inbox_unknown_patient)
    HealthifyCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(UnreadDotSize)
                        .clip(CircleShape)
                        .background(scheme.secondary),
                )
                Text(
                    text = stringResource(R.string.inbox_row_title, name, row.signal.asString()),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
            }
            row.receivedAt?.let { received ->
                Text(
                    text = stringResource(R.string.inbox_received_on, received.mediumDateText()),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val SKELETON_ROWS = 4

private val previewRows = listOf(
    InboxRow(1, "Ana Flores", UiText.Raw("desviación sostenida"), Instant.parse("2026-09-08T15:00:00Z")),
    InboxRow(2, "Carlos Quispe", UiText.Raw("señal de consistencia"), Instant.parse("2026-09-06T15:00:00Z")),
)

@Preview(name = "PR13 · Bandeja", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewInboxPreview() {
    HealthifyTheme {
        ReviewInboxContent(state = ReviewInboxUiState(isLoading = false, items = previewRows), onOpenItem = {}, onRetry = {})
    }
}

@Preview(name = "PR13 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewInboxLoadingPreview() {
    HealthifyTheme { ReviewInboxContent(state = ReviewInboxUiState(), onOpenItem = {}, onRetry = {}) }
}

@Preview(name = "PR13.V · Al día", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewInboxEmptyPreview() {
    HealthifyTheme {
        ReviewInboxContent(state = ReviewInboxUiState(isLoading = false, items = emptyList()), onOpenItem = {}, onRetry = {})
    }
}

@Preview(name = "PR13 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewInboxOfflinePreview() {
    HealthifyTheme {
        ReviewInboxContent(
            state = ReviewInboxUiState(isLoading = false, isOffline = true, loadFailed = true),
            onOpenItem = {},
            onRetry = {},
        )
    }
}

@Preview(name = "PR13 · Error", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewInboxErrorPreview() {
    HealthifyTheme {
        ReviewInboxContent(state = ReviewInboxUiState(isLoading = false, loadFailed = true), onOpenItem = {}, onRetry = {})
    }
}
