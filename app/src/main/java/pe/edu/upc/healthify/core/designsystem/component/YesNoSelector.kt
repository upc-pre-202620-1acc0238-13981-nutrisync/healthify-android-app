package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * «SelectorSiNo» del Figma: pregunta binaria **sin valor por defecto**. Reemplaza a los switches, que no
 * pueden representar «sin responder» (PT12 protocolo, PR14 ¿ajustaste el plan?).
 *
 * @param answer `null` = «SinResponder»; `true` = Sí; `false` = No.
 * @param isError muestra «Responde Sí o No» (estado «Error» del Figma, p. ej. al enviar sin responder).
 * @param errorText el texto del error si la pantalla tiene uno propio (PT12 «Responde si te pesaste en ayunas»).
 */
@Composable
fun YesNoSelector(
    question: String,
    answer: Boolean?,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val dimens = HealthifyTheme.dimens
    val showError = isError && answer == null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(text = question, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        SegmentedSelector(
            options = listOf(true, false),
            selected = answer,
            onSelect = onAnswer,
            optionLabel = { stringResource(if (it) R.string.ds_yes else R.string.ds_no) },
            enabled = enabled,
            isError = showError,
        )
        if (showError) {
            Row(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(dimens.space4),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = HealthifyIcons.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(dimens.iconSmall),
                )
                Text(
                    text = errorText ?: stringResource(R.string.ds_yes_no_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "SelectorSiNo · SinResponder/Sí/No/Error", widthDp = 360, heightDp = 520)
@Composable
private fun YesNoSelectorPreview() {
    ComponentPreview {
        YesNoSelector(question = "¿Pregunta?", answer = null, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = true, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = false, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = null, onAnswer = {}, isError = true)
    }
}
