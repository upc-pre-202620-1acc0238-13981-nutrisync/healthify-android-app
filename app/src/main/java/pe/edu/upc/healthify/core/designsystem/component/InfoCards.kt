package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Patient file avatar (48 dp in the Figma).
private val AvatarSize = 48.dp

/** Figma section header (e.g. "SINCE THE LAST CONSULTATION", "MANAGEMENT"): Overline/Section in `primary`. */
@Composable
fun SectionOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HealthifyTheme.extendedTypography.overlineSection,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Figma indicator card (160 × 100 dp in two-card rows): Body/Small label, Metric/Medium value and a supporting
 * line. [captionColor] `null` = `onSurfaceVariant` (the Figma uses `primary` on cards that open another view).
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    captionColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    HealthifyCard(modifier = modifier, shape = MaterialTheme.shapes.extraLarge, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Text(text = value, style = HealthifyTheme.extendedTypography.metricMedium, color = scheme.onSurface)
            Text(text = caption, style = MaterialTheme.typography.bodySmall, color = captionColor ?: scheme.onSurfaceVariant)
        }
    }
}

/** 24 dp radius "Card" container with 16 dp padding (the patient file and consultation cards). */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    verticalSpacing: Dp = HealthifyTheme.dimens.space12,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    HealthifyCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        borderColor = borderColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

/**
 * Figma icon row: 24 dp icon, Title/Small title and Body/Small text, with [trailing] on the right (chevron,
 * "Edit" button, a value…).
 */
@Composable
fun IconInfoRow(
    icon: ImageVector?,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(imageVector = icon, contentDescription = null, tint = scheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (supportingText != null) {
                Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        trailing?.invoke(this)
    }
}

/** List action row (e.g. "Schedule consultation ›"): icon, Body/Large text and chevron; 48 dp touch target. */
@Composable
fun ActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = scheme.onSurfaceVariant)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(imageVector = HealthifyIcons.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

/** Divider for lists inside a card (`outlineVariant`, 1 dp). */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = HealthifyTheme.dimens.borderThin,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Avatar with the initial (`primaryContainer` circle, Title/Large). */
@Composable
fun InitialAvatar(initial: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = MaterialTheme.typography.titleLarge, color = scheme.onPrimaryContainer)
    }
}

@Preview(name = "Info cards", widthDp = 360, heightDp = 520)
@Composable
private fun InfoCardsPreview() {
    ComponentPreview {
        SectionOverline("DESDE LA ÚLTIMA CONSULTA")
        Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space8)) {
            MetricCard("Peso (autopesaje)", "−0,3 kg/sem", "Tendencia · 4 semanas", Modifier.weight(1f))
            MetricCard("Cumplimiento", "5 de 7 días", "Últimos 7 días", Modifier.weight(1f))
        }
        SectionCard {
            IconInfoRow(icon = HealthifyIcons.Calendar, title = "Próxima consulta", supportingText = "Jueves 18 sept.")
            CardDivider()
            ActionRow(icon = HealthifyIcons.Agenda, text = "Agendar consulta", onClick = {})
        }
        InitialAvatar("A")
    }
}
