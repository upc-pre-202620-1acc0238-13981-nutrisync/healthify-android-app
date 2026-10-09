package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

/** Variantes «Style» del componente Card del Figma. */
enum class ListCardStyle { Elevated, Filled, Outlined }

// Medidas del componente Card del Figma.
private val ListCardMinHeight = 72.dp
private val LeadingContainerSize = 40.dp

/**
 * Contenedor «Card (contenedor)» del Figma: mismos tokens que [ListCard] para contenido libre (metas,
 * formularios). No agrega padding: el contenido define el suyo (normalmente 16 dp).
 *
 * @param onClick si no es nulo, toda la tarjeta es el área táctil y muestra el color «Pressed».
 * @param shape radio de la card: 16 dp por defecto; las cards de lista del diario y las ideas usan 24 dp
 *   (`MaterialTheme.shapes.extraLarge`).
 * @param borderColor contorno de 1 dp (p. ej. `tertiary` en las cards de IA); `null` = sin contorno propio.
 */
@Composable
fun HealthifyCard(
    modifier: Modifier = Modifier,
    style: ListCardStyle = ListCardStyle.Elevated,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = MaterialTheme.shapes.large,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    HealthifyCardImpl(
        modifier = modifier,
        style = style,
        onClick = onClick,
        interactionSource = interactionSource,
        forcePressed = false,
        shape = shape,
        borderColor = borderColor,
        content = content,
    )
}

@Composable
internal fun HealthifyCardImpl(
    modifier: Modifier = Modifier,
    style: ListCardStyle = ListCardStyle.Elevated,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    forcePressed: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.isPressed(forcePressed)
    val container = when {
        pressed -> HealthifyTheme.extendedColors.cardPressed
        style == ListCardStyle.Filled -> scheme.surfaceContainer
        else -> scheme.surfaceContainerLowest
    }
    Column(
        modifier = modifier
            .elevation(if (style == ListCardStyle.Elevated) dimens.elevation1 else null, shape)
            .clip(shape)
            .background(container)
            .then(
                when {
                    borderColor != null -> Modifier.border(dimens.borderThin, borderColor, shape)
                    style == ListCardStyle.Outlined -> Modifier.border(dimens.borderThin, scheme.outlineVariant, shape)
                    else -> Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}

/**
 * Card de lista del Figma (Elevated / Filled / Outlined): ícono inicial en círculo `primaryContainer`,
 * título, apoyo y chevron opcionales. Mín. 72 dp; toda la tarjeta es el área táctil.
 */
@Composable
fun ListCard(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    style: ListCardStyle = ListCardStyle.Elevated,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    interactionSource: MutableInteractionSource? = null,
) {
    ListCardImpl(title, modifier, supportingText, leadingIcon, style, onClick, showChevron, interactionSource)
}

@Composable
internal fun ListCardImpl(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    style: ListCardStyle = ListCardStyle.Elevated,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    interactionSource: MutableInteractionSource? = null,
    forcePressed: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCardImpl(
        modifier = modifier,
        style = style,
        onClick = onClick,
        interactionSource = interactionSource,
        forcePressed = forcePressed,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ListCardMinHeight)
                .padding(dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(LeadingContainerSize)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(dimens.icon),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                if (supportingText != null) {
                    Text(text = supportingText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = HealthifyIcons.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(dimens.icon),
                )
            }
        }
    }
}

@Preview(name = "Card · Style × State", widthDp = 360, heightDp = 640)
@Composable
private fun ListCardPreview() {
    ComponentPreview {
        ListCardStyle.entries.forEach { style ->
            ListCardImpl(
                title = "Título",
                supportingText = "Texto de apoyo",
                leadingIcon = HealthifyIcons.Folder,
                style = style,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            ListCardImpl(
                title = "Título",
                supportingText = "Texto de apoyo",
                leadingIcon = HealthifyIcons.Folder,
                style = style,
                onClick = {},
                forcePressed = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Card · contenedor y sin ícono", widthDp = 360)
@Composable
private fun HealthifyCardPreview() {
    ComponentPreview {
        ListCard(title = "Solo título", modifier = Modifier.fillMaxWidth())
        HealthifyCard(modifier = Modifier.fillMaxWidth(), style = ListCardStyle.Outlined) {
            Text(
                text = "Contenido libre",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(HealthifyTheme.dimens.space16),
            )
        }
    }
}
