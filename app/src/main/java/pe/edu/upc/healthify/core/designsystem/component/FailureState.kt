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

// «Grupo fijo» de PT7.2 / PT10.3 / PT14.4.E: círculo de 120 dp (error al 15 %) con uno de 84 dp (error) y «!».
private val OuterCircle = 120.dp
private val InnerCircle = 84.dp
private const val OUTER_ALPHA = 0.15f

/**
 * Estado «no se pudo» a pantalla completa (PT7.2 «NO SE PUDO REGISTRAR», PT10.3, PT14.4.E «HUBO UN ERROR»): insignia
 * de error, título (Headline/Medium), texto que aclara que no se perdió nada y la acción principal (Filled). El tono
 * nunca culpa al paciente.
 */
@Composable
fun FailureState(
    title: String,
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionLoading: Boolean = false,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(OuterCircle)
                .clip(CircleShape)
                .background(scheme.error.copy(alpha = OUTER_ALPHA))
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(InnerCircle)
                    .clip(CircleShape)
                    .background(scheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.ds_error_state_glyph),
                    style = MaterialTheme.typography.displaySmall,
                    color = scheme.onPrimary,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HealthifyButton(
            text = actionLabel,
            onClick = onAction,
            loading = actionLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (secondaryLabel != null) {
            HealthifyButton(
                text = secondaryLabel,
                onClick = onSecondary,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "FailureState", widthDp = 360, heightDp = 800)
@Composable
private fun FailureStatePreview() {
    ComponentPreview {
        FailureState(
            title = "NO SE PUDO REGISTRAR",
            text = "Revisa tu conexión e inténtalo de nuevo. Tu foto no se perdió.",
            actionLabel = "Volver a intentarlo",
            onAction = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
