package pe.edu.upc.healthify.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Variantes «Tipo» de Chip/Estado del Figma. Procedencia y confianza siempre visibles.
 * - [PendingConfirmation]: estimación de foto sin confirmar.
 * - [PendingSync]: en la cola offline, pendiente de enviar.
 */
enum class StatusChipType(@param:StringRes val labelRes: Int) {
    PendingConfirmation(R.string.status_chip_pending_confirmation),
    PendingSync(R.string.status_chip_pending_sync),
    Confirmed(R.string.status_chip_confirmed),
    Photo(R.string.status_chip_photo),
    Manual(R.string.status_chip_manual),
    OffPlan(R.string.status_chip_off_plan),
}

// Alto del Chip/Estado del Figma.
private val StatusChipMinHeight = 28.dp

/** Etiqueta de estado no interactiva (Chip/Estado del Figma). */
@Composable
fun StatusChip(
    type: StatusChipType,
    modifier: Modifier = Modifier,
    label: String = stringResource(type.labelRes),
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val (container: Color, content: Color) = when (type) {
        StatusChipType.PendingConfirmation -> scheme.secondaryContainer to scheme.onSecondaryContainer
        StatusChipType.Confirmed -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> scheme.surfaceContainer to scheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .heightIn(min = StatusChipMinHeight)
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(start = dimens.space8, end = dimens.space12)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = type.icon, contentDescription = null, tint = content, modifier = Modifier.size(dimens.iconSmall))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

private val StatusChipType.icon: ImageVector
    get() = when (this) {
        StatusChipType.PendingConfirmation -> HealthifyIcons.Clock
        StatusChipType.PendingSync -> HealthifyIcons.Sync
        StatusChipType.Confirmed -> HealthifyIcons.Check
        StatusChipType.Photo -> HealthifyIcons.Camera
        StatusChipType.Manual -> HealthifyIcons.Edit
        StatusChipType.OffPlan -> HealthifyIcons.Cutlery
    }

@Preview(name = "Chip/Estado · todos los tipos", widthDp = 360)
@Composable
private fun StatusChipPreview() {
    ComponentPreview {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
            verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8),
        ) {
            StatusChipType.entries.forEach { StatusChip(type = it) }
        }
    }
}
