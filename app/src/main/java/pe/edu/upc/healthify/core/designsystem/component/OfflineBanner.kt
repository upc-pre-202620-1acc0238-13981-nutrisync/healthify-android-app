package pe.edu.upc.healthify.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Variantes «Contexto» del Banner/SinConexión del Figma.
 * - [Queueable]: lo registrado se encola y se envía después (diario, autopesaje, fuera del plan).
 * - [RequiresNetwork]: la acción necesita red (registro, login, acciones del nutricionista).
 */
enum class OfflineBannerType(@param:StringRes val messageRes: Int) {
    Queueable(R.string.ds_offline_queueable),
    RequiresNetwork(R.string.ds_offline_requires_network),
}

/**
 * Aviso persistente de conectividad bajo la TopAppBar (reemplaza al Toast «Sin conexión» del Figma).
 * Se anuncia a TalkBack al aparecer.
 */
@Composable
fun OfflineBanner(type: OfflineBannerType, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.secondaryContainer)
            .padding(horizontal = dimens.space16, vertical = dimens.space12)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = HealthifyIcons.WifiOff,
            contentDescription = null,
            tint = scheme.onSecondaryContainer,
            modifier = Modifier.size(dimens.icon),
        )
        Text(
            text = stringResource(type.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSecondaryContainer,
        )
    }
}

@Preview(name = "Banner/SinConexión · Encolable / RequiereRed", widthDp = 360)
@Composable
private fun OfflineBannerPreview() {
    ComponentPreview {
        OfflineBannerType.entries.forEach { OfflineBanner(type = it) }
    }
}
