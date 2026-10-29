package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

// Figma BottomSheet measurements.
private val HandleSize = DpSize(32.dp, 4.dp)
private val HandleRadius = 2.dp
private const val SHEET_SCRIM_ALPHA = 0.32f

/**
 * Figma modal BottomSheet: `surfaceContainerLowest`, 28 dp top radius, 32×4 dp handle.
 * The standard content is [BottomSheetContent]. It closes with [onDismissRequest] (swipe, tap the
 * scrim or back); the screen hides it by no longer composing it.
 */
// The SheetState (experimental M3 API) is not exposed, so screens do not have to opt in to
// experimental APIs. The sheet always opens expanded.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthifyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = dimens.radiusDialog, topEnd = dimens.radiusDialog),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = dimens.space0,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SHEET_SCRIM_ALPHA),
        dragHandle = { BottomSheetHandle(Modifier.padding(top = dimens.space12)) },
        content = content,
    )
}

/** Figma BottomSheet handle. */
@Composable
fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(HandleSize)
            .clip(RoundedCornerShape(HandleRadius))
            .background(MaterialTheme.colorScheme.outline),
    )
}

/**
 * Standard Figma BottomSheet content: title (Headline/Small), supporting text, full-width Filled main
 * action and optional Text secondary action.
 */
@Composable
fun BottomSheetContent(
    title: String,
    text: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    primaryLoading: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = primaryLabel,
            onClick = onPrimary,
            loading = primaryLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (secondaryLabel != null) {
            HealthifyButton(text = secondaryLabel, onClick = onSecondary, style = HealthifyButtonStyle.Text)
        }
    }
}

/** Static copy of the sheet (no window) for previews and the catalog. */
@Composable
internal fun BottomSheetStaticFrame(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dimens = HealthifyTheme.dimens
    val shape = RoundedCornerShape(topStart = dimens.radiusDialog, topEnd = dimens.radiusDialog)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .elevation(dimens.elevation3, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BottomSheetHandle(Modifier.padding(top = dimens.space12))
        content()
    }
}

@Preview(name = "BottomSheet", widthDp = 360)
@Composable
private fun BottomSheetPreview() {
    ComponentPreview {
        BottomSheetStaticFrame {
            BottomSheetContent(
                title = "TÍTULO",
                text = "Texto de apoyo.",
                primaryLabel = "Aceptar",
                onPrimary = {},
                secondaryLabel = "Más tarde",
            )
        }
    }
}
