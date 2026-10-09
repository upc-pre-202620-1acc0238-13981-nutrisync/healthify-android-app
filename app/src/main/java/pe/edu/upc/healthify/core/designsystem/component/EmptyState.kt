package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Estado vacío del Figma: bloque centrado con título (Title/Medium), texto de apoyo y acción opcional
 * (Tonal, ancho completo). Pasa `Modifier.fillMaxSize()` (o `weight`) para centrarlo en el área disponible.
 * Tono de invitación, nunca de reproche.
 */
@Composable
fun EmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionStyle: HealthifyButtonStyle = HealthifyButtonStyle.Tonal,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.space16, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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
        }
        if (actionLabel != null) {
            HealthifyButton(
                text = actionLabel,
                onClick = onAction,
                style = actionStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "EmptyState · con y sin acción", widthDp = 360, heightDp = 520)
@Composable
private fun EmptyStatePreview() {
    ComponentPreview {
        EmptyState(
            title = "Necesitas conexión",
            text = "Tu cartera y los datos de tus pacientes no se guardan en el teléfono. " +
                "Vuelve a intentarlo cuando tengas conexión.",
            actionLabel = "Reintentar",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
        EmptyState(
            title = "Aún no hay registros",
            text = "Cuando registres tu primera comida aparecerá aquí.",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
