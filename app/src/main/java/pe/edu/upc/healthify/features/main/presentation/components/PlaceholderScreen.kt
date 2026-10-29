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

/** Extra action of a placeholder (temporary shortcuts while the real screen does not exist). */
data class PlaceholderAction(val label: String, val onClick: () -> Unit)

/**
 * Placeholder for a route whose screen is not implemented yet (shell tabs).
 *
 * While the Settings screens do not exist, the Settings placeholders offer "Sign out" so the person can
 * leave the account, and the patient's one also offers more shortcuts ([extraActions]).
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

@Preview(name = "Screen placeholder", widthDp = 360, heightDp = 800)
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
