package pe.edu.upc.healthify.features.onboarding.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

// Medidas de «chip/meta», «chip/registro» y «chip/nutri» del frame S2.
private val BadgeSize = 36.dp
private val BadgeIconSize = 18.dp
private val TargetRingStroke = 2.dp
private val TargetRingInset = 2.dp
private val TargetDotRadius = 2.dp

/** Ícono de cada chip ilustrativo de S2. */
sealed interface WelcomeChipIcon {
    /** «Meta de hoy»: diana (anillo con punto), dibujada como en el Figma. */
    data object Target : WelcomeChipIcon
    data class Vector(val imageVector: ImageVector) : WelcomeChipIcon
}

/**
 * Tarjeta ilustrativa inclinada de S2 (no es interactiva ni muestra datos reales).
 *
 * @param rotation grados en sentido horario (el Figma los da en sentido antihorario).
 */
@Composable
fun WelcomeChip(
    label: String,
    value: String,
    icon: WelcomeChipIcon,
    badgeColor: Color,
    iconColor: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = modifier
            .rotate(rotation)
            .elevation(dimens.elevation1, shape)
            .background(scheme.surfaceContainerLowest, shape)
            .padding(start = dimens.space12, top = dimens.space8, end = dimens.space16, bottom = dimens.space8),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .background(badgeColor, shape),
            contentAlignment = Alignment.Center,
        ) {
            when (icon) {
                WelcomeChipIcon.Target -> TargetIcon(color = iconColor)
                is WelcomeChipIcon.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(BadgeIconSize),
                )
            }
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        }
    }
}

@Composable
private fun TargetIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(BadgeIconSize)) {
        val stroke = TargetRingStroke.toPx()
        val ringRadius = size.minDimension / 2 - TargetRingInset.toPx()
        drawCircle(color = color, radius = ringRadius, style = Stroke(width = stroke))
        drawCircle(color = color, radius = TargetDotRadius.toPx())
    }
}

@Preview(name = "S2 · chips", widthDp = 360)
@Composable
private fun WelcomeChipPreview() {
    HealthifyTheme {
        val scheme = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .background(scheme.inverseSurface)
                .padding(HealthifyTheme.dimens.space24),
            verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space24),
        ) {
            WelcomeChip("Meta de hoy", "1 260 / 1 850 kcal", WelcomeChipIcon.Target, scheme.secondary, scheme.surfaceContainerLowest, -6f)
            WelcomeChip(
                "Comida registrada",
                "Lomo saltado · 320 g",
                WelcomeChipIcon.Vector(HealthifyIcons.Camera),
                scheme.primary,
                scheme.surfaceContainerLowest,
                5f,
            )
        }
    }
}
