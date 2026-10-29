package pe.edu.upc.healthify.features.carerelationship.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogContent
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * "Switch nutritionist?". A `dialog` destination of the root graph over Settings: "Scan invitation" opens the
 * scan screen with `replaceActiveLink = true` and "Cancel" returns to Settings. The previous care link closes only
 * once the new invitation is accepted.
 */
@Composable
fun SwitchPractitionerDialogContent(
    onScanInvitation: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HealthifyDialogContent(
        title = stringResource(R.string.switch_practitioner_title),
        text = stringResource(R.string.switch_practitioner_body),
        confirmLabel = stringResource(R.string.switch_practitioner_scan),
        onConfirm = onScanInvitation,
        onDismiss = onCancel,
        modifier = modifier,
    )
}

@Preview(name = "Switch nutritionist?", widthDp = 360, heightDp = 400)
@Composable
private fun SwitchPractitionerDialogPreview() {
    HealthifyTheme { SwitchPractitionerDialogContent(onScanInvitation = {}, onCancel = {}) }
}
