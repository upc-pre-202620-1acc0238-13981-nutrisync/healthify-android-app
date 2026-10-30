package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

private val BoxSize = 20.dp

/**
 * Visual checkbox, drawn like [HealthifySwitch]: checked = `primary` fill with an `onPrimary` check; unchecked =
 * `outline` border (`error` when [isError]); disabled = disabled colors.
 *
 * Used inside [HealthifyCheckboxRow], which makes the whole row tappable (≥ 48 dp) and announces the state to TalkBack.
 */
@Composable
fun HealthifyCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val ext = HealthifyTheme.extendedColors
    val shape = MaterialTheme.shapes.extraSmall
    val fill = when {
        !enabled && checked -> ext.disabledContainer
        checked -> scheme.primary
        else -> scheme.surfaceContainerLowest
    }
    val border = when {
        !enabled -> ext.disabledContainer
        checked -> scheme.primary
        isError -> scheme.error
        else -> scheme.outline
    }
    Box(
        modifier = modifier
            .size(BoxSize)
            .clip(shape)
            .background(fill)
            .border(dimens.borderThick, border, shape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = HealthifyIcons.Check,
                contentDescription = null,
                tint = if (enabled) scheme.onPrimary else ext.onDisabled,
                modifier = Modifier.size(dimens.iconSmall),
            )
        }
    }
}

/**
 * "Checkbox + label" row (12 dp gap); the whole row is the touch target. [label] is an [AnnotatedString] so it can
 * carry links (`LinkAnnotation`): tapping a link opens it instead of toggling the box.
 */
@Composable
fun HealthifyCheckboxRow(
    label: AnnotatedString,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HealthifyCheckbox(checked = checked, enabled = enabled, isError = isError)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(name = "Checkbox · checked / unchecked / error / disabled", widthDp = 360)
@Composable
private fun CheckboxPreview() {
    ComponentPreview {
        HealthifyCheckboxRow(label = AnnotatedString("Acepto los términos"), checked = true, onCheckedChange = {})
        HealthifyCheckboxRow(label = AnnotatedString("Acepto los términos"), checked = false, onCheckedChange = {})
        HealthifyCheckboxRow(
            label = AnnotatedString("Acepto los términos"),
            checked = false,
            onCheckedChange = {},
            isError = true,
        )
        HealthifyCheckboxRow(
            label = AnnotatedString("Acepto los términos"),
            checked = true,
            onCheckedChange = {},
            enabled = false,
        )
    }
}
