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
 * Estado «Pressed» de los componentes del Figma. Los componentes pintan el color sólido de presionado del
 * Figma (p. ej. `secondaryPressed`) en lugar del ripple de M3. [forced] solo lo usan las previews y el catálogo
 * para mostrar la variante «Pressed» sin tocar la pantalla.
 */
@Composable
internal fun InteractionSource.isPressed(forced: Boolean): Boolean {
    val pressed by collectIsPressedAsState()
    return forced || pressed
}

/** Fondo `surface` y margen de pantalla para las previews de componentes. */
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
