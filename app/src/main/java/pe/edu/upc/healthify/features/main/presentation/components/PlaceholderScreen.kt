package pe.edu.upc.healthify.features.main.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/** Acción extra de un marcador (accesos provisionales mientras no exista la pantalla real). */
data class PlaceholderAction(val label: String, val onClick: () -> Unit)

/**
 * Marcador de una ruta cuya pantalla aún no se implementa (pestañas de los shells).
 *
 * DECISIÓN shells: mientras no existan PT21/PR20 (Ajustes), los marcadores de Ajustes ofrecen «Cerrar sesión» para
 * poder salir de la cuenta, y el del paciente además PT21.V y PT23 ([extraActions]).
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    extraActions: List<PlaceholderAction> = emptyList(),
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HealthifyTopAppBar(title = title, windowInsets = WindowInsets.statusBars)
        EmptyState(
            title = title,
            text = stringResource(R.string.placeholder_body),
            actionLabel = onSignOut?.let { stringResource(R.string.placeholder_sign_out) },
            onAction = { onSignOut?.invoke() },
            actionStyle = HealthifyButtonStyle.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = dimens.screenHorizontal),
        )
        extraActions.forEach { action ->
            HealthifyButton(
                text = action.label,
                onClick = action.onClick,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenHorizontal),
            )
        }
    }
}

@Preview(name = "Marcador de pantalla", widthDp = 360, heightDp = 800)
@Composable
private fun PlaceholderScreenPreview() {
    HealthifyTheme {
        PlaceholderScreen(
            title = "Ajustes",
            onSignOut = {},
            extraActions = listOf(PlaceholderAction("Vincularme con otro nutricionista") {}),
        )
    }
}
