package pe.edu.upc.healthify.features.carerelationship.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogContent
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * PT21.V · «¿Cambiar de nutricionista?». Es un destino `dialog` del grafo raíz sobre Ajustes: «Escanear invitación»
 * abre PT1 con `replaceActiveLink = true` (CR-1) y «Cancelar» vuelve a Ajustes. El vínculo anterior se cierra solo
 * cuando la nueva invitación se acepta.
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

@Preview(name = "PT21.V · ¿Cambiar de nutricionista?", widthDp = 360, heightDp = 400)
@Composable
private fun SwitchPractitionerDialogPreview() {
    HealthifyTheme { SwitchPractitionerDialogContent(onScanInvitation = {}, onCancel = {}) }
}
