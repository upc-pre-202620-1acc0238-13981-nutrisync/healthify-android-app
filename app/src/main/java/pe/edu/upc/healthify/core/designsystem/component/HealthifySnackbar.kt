package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

/**
 * SnackbarHost con el Snackbar del Figma. Va en el `snackbarHost` del Scaffold (aparece sobre la
 * NavigationBar). Mostrar con `hostState.showSnackbar(message, actionLabel)`: la duración por defecto
 * (`SnackbarDuration.Short`) son los 4 s del Figma.
 */
@Composable
fun HealthifySnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> HealthifySnackbar(data) }
}

/** Snackbar del Figma para un [SnackbarData] de un [SnackbarHost]. */
@Composable
fun HealthifySnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    HealthifySnackbarContent(
        message = data.visuals.message,
        modifier = modifier,
        actionLabel = data.visuals.actionLabel,
        onAction = data::performAction,
    )
}

/**
 * Snackbar del Figma (`inverseSurface`, radio 4 dp, Elevation/3): «Simple» o «ConAcción» (acción de
 * 48 dp en `inversePrimary`).
 */
@Composable
fun HealthifySnackbarContent(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .padding(horizontal = dimens.screenHorizontal, vertical = dimens.space8)
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .elevation(dimens.elevation3, shape)
            .clip(shape)
            .background(scheme.inverseSurface)
            .padding(start = dimens.space16, end = dimens.space8, top = dimens.space4, bottom = dimens.space4)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.inverseOnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = dimens.space8),
        )
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = dimens.touchMin)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = dimens.space12),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge, color = scheme.inversePrimary)
            }
        }
    }
}

@Preview(name = "Snackbar · Simple / ConAcción", widthDp = 360)
@Composable
private fun SnackbarPreview() {
    ComponentPreview {
        HealthifySnackbarContent(message = "Comida registrada en tu diario")
        HealthifySnackbarContent(message = "Comida registrada en tu diario", actionLabel = "Ver diario")
    }
}
