package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

// Diálogo con ícono del Figma (EV-2.S): 312 dp de ancho, círculo de 56 dp con el ícono de 28 dp.
private val IconDialogMaxWidth = 312.dp
private val IconCircleSize = 56.dp
private val IconGlyphSize = 28.dp

/**
 * Diálogo con ícono del Figma (EV-2.S «¿Salir de la consulta?»): círculo `secondaryContainer` con el ícono, título en
 * Metric/Medium, texto de apoyo y dos acciones apiladas (Filled + Text).
 */
@Composable
fun HealthifyIconDialog(
    icon: ImageVector,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss, properties = HealthifyDialogProperties) {
        HealthifyDialogScrim()
        HealthifyIconDialogContent(icon, title, text, confirmLabel, onConfirm, dismissLabel, onDismiss, modifier)
    }
}

/** Superficie del diálogo con ícono sin ventana (previews). */
@Composable
fun HealthifyIconDialogContent(
    icon: ImageVector,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusXl)
    Column(
        modifier = modifier
            .padding(horizontal = dimens.space24)
            .widthIn(max = IconDialogMaxWidth)
            .fillMaxWidth()
            .elevation(dimens.elevation3, shape)
            .clip(shape)
            .background(scheme.surfaceContainerLowest)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.space16, vertical = dimens.space24)
            .semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconCircleSize)
                .clip(CircleShape)
                .background(scheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(IconGlyphSize))
        }
        Text(
            text = title,
            style = HealthifyTheme.extendedTypography.metricMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        HealthifyButton(text = confirmLabel, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        HealthifyButton(
            text = dismissLabel,
            onClick = onDismiss,
            style = HealthifyButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Diálogo con ícono", widthDp = 360, heightDp = 520)
@Composable
private fun HealthifyIconDialogPreview() {
    ComponentPreview {
        HealthifyIconDialogContent(
            icon = HealthifyIcons.Warning,
            title = "¿SALIR DE LA CONSULTA?",
            text = "Lo que ya guardaste queda guardado.",
            confirmLabel = "Salir y continuar después",
            onConfirm = {},
            dismissLabel = "Seguir aquí",
            onDismiss = {},
        )
    }
}
