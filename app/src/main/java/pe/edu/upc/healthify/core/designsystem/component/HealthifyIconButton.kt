package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

/** Variantes «Style» del IconButton del Figma. */
enum class HealthifyIconButtonStyle { Standard, Tonal, Filled }

/**
 * IconButton del Figma: área táctil 48×48 dp, ícono 24 dp.
 *
 * @param contentDescription obligatorio salvo que el ícono sea decorativo y otro elemento lo describa.
 */
@Composable
fun HealthifyIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HealthifyIconButtonStyle = HealthifyIconButtonStyle.Standard,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    tint: Color? = null,
) {
    HealthifyIconButtonImpl(icon, contentDescription, onClick, modifier, style, enabled, interactionSource, tint = tint)
}

@Composable
internal fun HealthifyIconButtonImpl(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HealthifyIconButtonStyle = HealthifyIconButtonStyle.Standard,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    forcePressed: Boolean = false,
    tint: Color? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.isPressed(forcePressed) && enabled
    val container = when (style) {
        HealthifyIconButtonStyle.Standard -> if (pressed) ext.primaryOverlayPressed else Color.Transparent
        HealthifyIconButtonStyle.Tonal -> when {
            !enabled -> ext.disabledContainer
            pressed -> ext.primaryContainerPressed
            else -> scheme.primaryContainer
        }
        HealthifyIconButtonStyle.Filled -> when {
            !enabled -> ext.disabledContainer
            pressed -> ext.cardPressed
            else -> scheme.surfaceContainerLowest
        }
    }
    val iconTint = when {
        !enabled -> ext.onDisabled
        tint != null -> tint
        style == HealthifyIconButtonStyle.Tonal -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    Box(
        modifier = modifier
            .size(dimens.touchMin)
            .elevation(if (style == HealthifyIconButtonStyle.Filled && enabled) dimens.elevation1 else null, CircleShape)
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(dimens.icon))
    }
}

@Preview(name = "IconButton · estilos × estados", widthDp = 360)
@Composable
private fun IconButtonPreview() {
    ComponentPreview {
        HealthifyIconButtonStyle.entries.forEach { style ->
            Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space16)) {
                HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style)
                HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style, forcePressed = true)
                HealthifyIconButtonImpl(HealthifyIcons.ArrowBack, null, {}, style = style, enabled = false)
            }
        }
    }
}
