package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

// Figma switch: 40 × 24 dp track, 4 dp padding, 16 dp thumb.
private val TrackWidth = 40.dp
private val TrackHeight = 24.dp
private val ThumbSize = 16.dp
private val ThumbTravel = TrackWidth - ThumbSize - 8.dp

/**
 * Visual switch. The Figma only draws the on state (`primary` track, `surfaceContainerLowest` thumb on
 * the right). Design system choice: off = `surfaceContainerHigh` track and `outline` thumb on the
 * left; disabled = disabled colors.
 *
 * Used inside [HealthifySwitchRow], which makes the whole row tappable (≥ 48 dp) and announces the state to TalkBack.
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

/**
 * Figma "label + switch" row (24 dp gap); the whole row is the touch target.
 *
 * @param supportingText secondary text under the label (e.g. a reminder's time), Body/Small in
 *   `onSurfaceVariant`. Label and text sit together in a column, centered against the switch.
 */
@Composable
fun HealthifySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
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
        Column(
            modifier = Modifier.weight(1f).padding(vertical = if (supportingText != null) dimens.space4 else dimens.space0),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HealthifySwitch(checked = checked, enabled = enabled)
    }
}

@Preview(name = "Switch · on / off / disabled", widthDp = 360)
@Composable
private fun SwitchPreview() {
    ComponentPreview {
        HealthifySwitchRow(label = "Usar funciones con IA", checked = true, onCheckedChange = {})
        HealthifySwitchRow(label = "Usar funciones con IA", checked = false, onCheckedChange = {})
        HealthifySwitchRow(label = "Usar funciones con IA", checked = false, onCheckedChange = {}, enabled = false)
    }
}

@Preview(name = "Switch · with supporting text", widthDp = 360)
@Composable
private fun SwitchSupportingPreview() {
    ComponentPreview {
        HealthifySwitchRow(label = "Recordatorio de pesaje", supportingText = "7:30", checked = true, onCheckedChange = {})
        HealthifySwitchRow(label = "Recordatorio de comidas", supportingText = "13:00 y 20:00", checked = false, onCheckedChange = {})
    }
}
