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
 * Figma yes/no selector ("SelectorSiNo"): binary question **with no default value**. Replaces switches,
 * which cannot represent "unanswered" (e.g. the weigh-in protocol, "did you adjust the plan?").
 *
 * @param answer `null` = unanswered; `true` = Yes; `false` = No.
 * @param isError shows "Answer Yes or No" (the Figma "Error" state, e.g. on submit without an answer).
 * @param errorText the error text if the screen has its own (e.g. the weigh-in's fasting question).
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

@Preview(name = "YesNoSelector · Unanswered/Yes/No/Error", widthDp = 360, heightDp = 520)
@Composable
private fun YesNoSelectorPreview() {
    ComponentPreview {
        YesNoSelector(question = "¿Pregunta?", answer = null, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = true, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = false, onAnswer = {})
        YesNoSelector(question = "¿Pregunta?", answer = null, onAnswer = {}, isError = true)
    }
}
