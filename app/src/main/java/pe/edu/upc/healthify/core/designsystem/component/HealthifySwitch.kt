package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Interruptor del Figma («switch» de PT2, PT21.IA y PT22): pista 40 × 24 dp, padding 4 dp, perilla 16 dp.
private val TrackWidth = 40.dp
private val TrackHeight = 24.dp
private val ThumbSize = 16.dp
private val ThumbTravel = TrackWidth - ThumbSize - 8.dp

/**
 * Interruptor visual. El Figma solo dibuja el estado encendido (pista `primary`, perilla `surfaceContainerLowest` a
 * la derecha). DECISIÓN Sistema de diseño: apagado = pista `surfaceContainerHigh` y perilla `outline` a la
 * izquierda; deshabilitado = colores de deshabilitado.
 *
 * Se usa dentro de [HealthifySwitchRow], que hace tocable toda la fila (≥ 48 dp) y anuncia el estado a TalkBack.
 */
@Composable
fun HealthifySwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    val track = when {
        !enabled -> ext.disabledContainer
        checked -> scheme.primary
        else -> scheme.surfaceContainerHigh
    }
    val thumb = when {
        !enabled -> ext.onDisabled
        checked -> scheme.surfaceContainerLowest
        else -> scheme.outline
    }
    val offset by animateDpAsState(targetValue = if (checked) ThumbTravel else 0.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .clip(CircleShape)
            .background(track)
            .padding(HealthifyTheme.dimens.space4)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToPx(), 0) }
                .size(ThumbSize)
                .clip(CircleShape)
                .background(thumb),
        )
    }
}

/** Fila «etiqueta + interruptor» del Figma (24 dp de separación); toda la fila es el área táctil. */
@Composable
fun HealthifySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = HealthifyTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(dimens.space24),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        HealthifySwitch(checked = checked, enabled = enabled)
    }
}

@Preview(name = "Switch · encendido / apagado / deshabilitado", widthDp = 360)
@Composable
private fun SwitchPreview() {
    ComponentPreview {
        HealthifySwitchRow(label = "Usar funciones con IA", checked = true, onCheckedChange = {})
        HealthifySwitchRow(label = "Usar funciones con IA", checked = false, onCheckedChange = {})
        HealthifySwitchRow(label = "Usar funciones con IA", checked = false, onCheckedChange = {}, enabled = false)
    }
}
