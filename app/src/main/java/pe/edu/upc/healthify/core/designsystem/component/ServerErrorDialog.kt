package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pe.edu.upc.healthify.R

/**
 * "Generic server error" dialog ("Retry" / "Close") for the sign-up, sign-in, consent and consent withdrawal
 * validations. Uses the texts of the design system's generic error state.
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
