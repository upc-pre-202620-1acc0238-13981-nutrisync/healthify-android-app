package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation

// Dialog del Figma: ancho 312 dp en el frame de 360 (24 dp por lado); scrim al 32 %.
private val DialogMaxWidth = 312.dp
private const val DIALOG_SCRIM_ALPHA = 0.32f

/**
 * AlertDialog del Figma («Básico» / «Destructivo»). El gesto atrás y tocar fuera equivalen a Cancelar
 * ([onDismiss]).
 *
 * @param dismissLabel `null` = diálogo informativo con un solo botón ([confirmLabel]).
 * @param destructive el botón de confirmación usa el estilo Danger (acciones irreversibles).
 */
@Composable
fun HealthifyDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = stringResource(R.string.ds_cancel),
    destructive: Boolean = false,
    confirmLoading: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss, properties = HealthifyDialogProperties) {
        HealthifyDialogScrim()
        HealthifyDialogContent(
            title = title,
            text = text,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
            dismissLabel = dismissLabel,
            destructive = destructive,
            confirmLoading = confirmLoading,
        )
    }
}

/**
 * Scrim del Figma (32 %) para la ventana del diálogo actual. Lo usan [HealthifyDialog] y los destinos `dialog` de
 * Navigation que muestran un [HealthifyDialogContent].
 */
@Composable
fun HealthifyDialogScrim() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(DIALOG_SCRIM_ALPHA) }
}

/** Ventana de diálogo sin el ancho por defecto de la plataforma (el ancho lo pone [HealthifyDialogContent]). */
val HealthifyDialogProperties = DialogProperties(usePlatformDefaultWidth = false)

/** Superficie del diálogo sin ventana (previews, catálogo y diálogos con contenido propio). */
@Composable
fun HealthifyDialogContent(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = stringResource(R.string.ds_cancel),
    destructive: Boolean = false,
    confirmLoading: Boolean = false,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(dimens.radiusDialog)
    Column(
        modifier = modifier
            .padding(horizontal = dimens.space24)
            .widthIn(max = DialogMaxWidth)
            .fillMaxWidth()
            .elevation(dimens.elevation3, shape)
            .clip(shape)
            .background(scheme.surfaceContainerLowest)
            .verticalScroll(rememberScrollState())
            .padding(start = dimens.space24, top = dimens.space24, end = dimens.space24, bottom = dimens.space16)
            .semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space8, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dismissLabel != null) {
                HealthifyButton(text = dismissLabel, onClick = onDismiss, style = HealthifyButtonStyle.Text)
            }
            HealthifyButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (destructive) HealthifyButtonStyle.Danger else HealthifyButtonStyle.Text,
                loading = confirmLoading,
            )
        }
    }
}

@Preview(name = "Dialog · Básico / Destructivo", widthDp = 360, heightDp = 520)
@Composable
private fun DialogPreview() {
    ComponentPreview {
        HealthifyDialogContent(
            title = "¿Título del diálogo?",
            text = "Texto de apoyo que explica la consecuencia de la acción.",
            confirmLabel = "Confirmar",
            onConfirm = {},
            onDismiss = {},
        )
        HealthifyDialogContent(
            title = "¿Título del diálogo?",
            text = "Texto de apoyo que explica la consecuencia de la acción.",
            confirmLabel = "Confirmar",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
        )
    }
}
