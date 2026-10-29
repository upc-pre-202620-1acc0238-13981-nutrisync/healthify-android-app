package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * "Pressed" state of the Figma components. Components paint the Figma's solid pressed color
 * (e.g. `secondaryPressed`) instead of the M3 ripple. [forced] is only used by previews and the catalog
 * to show the "Pressed" variant without touching the screen.
 */
@Composable
internal fun InteractionSource.isPressed(forced: Boolean): Boolean {
    val pressed by collectIsPressedAsState()
    return forced || pressed
}

/** `surface` background and screen margin for component previews. */
@Composable
internal fun ComponentPreview(content: @Composable ColumnScope.() -> Unit) {
    HealthifyTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(HealthifyTheme.dimens.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12),
                content = content,
            )
        }
    }
}
