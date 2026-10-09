package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * «Filtro» del Figma (PT25.2): chip de selección múltiple. Seleccionado = `primaryContainer`, borde `primary` y check;
 * sin seleccionar = borde `outline`. Visual de 38 dp con área táctil de 48 dp.
 */
@Composable
fun HealthifyFilterChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLowest)
            .border(dimens.borderThin, if (selected) scheme.primary else scheme.outline, shape)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = onSelectedChange)
            .padding(horizontal = dimens.space12, vertical = dimens.space8),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = HealthifyIcons.Check,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(dimens.iconSmall),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        )
    }
}

/** «Etiqueta» del Figma (PT20 Mi plan): texto con borde `outline`, solo lectura. */
@Composable
fun HealthifyTag(text: String, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val shape = MaterialTheme.shapes.small
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(dimens.borderThin, MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = dimens.space12, vertical = dimens.space4),
    )
}

@Preview(name = "Filtro · Etiqueta", widthDp = 360)
@Composable
private fun ChipsPreview() {
    ComponentPreview {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            HealthifyFilterChip(text = "Cenas", selected = true, onSelectedChange = {})
            HealthifyFilterChip(text = "Comer fuera", selected = false, onSelectedChange = {})
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            HealthifyTag(text = "Prioriza vegetales")
            HealthifyTag(text = "Sin mariscos")
        }
    }
}
