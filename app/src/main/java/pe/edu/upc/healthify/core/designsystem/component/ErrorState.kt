package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Medidas del estado de error del Figma (S5 · Cargando — Error).
private val BadgeSize = 88.dp
private const val BADGE_ALPHA = 0.15f

/**
 * Estado de error genérico (patrón de S5 «Cargando — Error»): insignia «!», título, texto y «Reintentar»
 * (Filled, ancho completo) con una acción secundaria Outlined opcional. Los textos por defecto son los
 * genéricos; cada pantalla pasa los de su frame «Notas» cuando los tenga.
 */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.ds_error_state_title),
    text: String = stringResource(R.string.ds_error_state_body),
    retryLabel: String = stringResource(R.string.ds_retry),
    retrying: Boolean = false,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.space24, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .clip(CircleShape)
                .background(scheme.secondary.copy(alpha = BADGE_ALPHA))
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.ds_error_state_glyph),
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onSecondaryContainer,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = retryLabel,
                onClick = onRetry,
                loading = retrying,
                modifier = Modifier.fillMaxWidth(),
            )
            if (secondaryLabel != null) {
                HealthifyButton(
                    text = secondaryLabel,
                    onClick = onSecondary,
                    style = HealthifyButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "ErrorState", widthDp = 360, heightDp = 800)
@Composable
private fun ErrorStatePreview() {
    ComponentPreview {
        ErrorState(onRetry = {}, secondaryLabel = "Cerrar", modifier = Modifier.fillMaxWidth())
    }
}
