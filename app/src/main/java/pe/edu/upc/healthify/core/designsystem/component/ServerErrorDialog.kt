package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R

/**
 * «Error de servidor genérico» («Reintentar» / «Cerrar») de las validaciones de S3, S4, PT2 y PT23. Usa los
 * textos del estado de error genérico del sistema de diseño (S5 · Error).
 */
@Composable
fun ServerErrorDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HealthifyDialog(
        title = stringResource(R.string.ds_error_state_title),
        text = stringResource(R.string.ds_error_state_body),
        confirmLabel = stringResource(R.string.ds_retry),
        onConfirm = onRetry,
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.common_close),
        modifier = modifier,
    )
}
