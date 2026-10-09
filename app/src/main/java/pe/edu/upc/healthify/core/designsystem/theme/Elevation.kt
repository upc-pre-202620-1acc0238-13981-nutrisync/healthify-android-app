package pe.edu.upc.healthify.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Una sombra del estilo de efecto del Figma (offset Y, desenfoque y opacidad del negro). */
@Immutable
data class HealthifyShadowLayer(val offsetY: Dp, val blur: Dp, val alpha: Float)

/**
 * Estilos de efecto «Elevation/1–3» del Figma. Se dibujan con sombras reales ([dropShadow]) en lugar de
 * la elevación tonal de M3, para que coincidan con el diseño.
 */
@Immutable
data class HealthifyElevation(val layers: List<HealthifyShadowLayer>) {
    companion object {
        val Level1 = HealthifyElevation(
            listOf(
                HealthifyShadowLayer(offsetY = 1.dp, blur = 3.dp, alpha = 0.12f),
                HealthifyShadowLayer(offsetY = 1.dp, blur = 2.dp, alpha = 0.08f),
            ),
        )
        val Level2 = HealthifyElevation(listOf(HealthifyShadowLayer(offsetY = 2.dp, blur = 6.dp, alpha = 0.14f)))
        val Level3 = HealthifyElevation(listOf(HealthifyShadowLayer(offsetY = 4.dp, blur = 12.dp, alpha = 0.18f)))
    }
}

/** Dibuja la elevación del Figma detrás del contenido. Va antes de `background`/`clip` en la cadena. */
fun Modifier.elevation(elevation: HealthifyElevation?, shape: Shape = RectangleShape): Modifier {
    if (elevation == null) return this
    return elevation.layers.fold(this) { acc, layer ->
        acc.dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = layer.blur,
                color = Neutral1000,
                offset = DpOffset(0.dp, layer.offsetY),
                alpha = layer.alpha,
            ),
        )
    }
}
