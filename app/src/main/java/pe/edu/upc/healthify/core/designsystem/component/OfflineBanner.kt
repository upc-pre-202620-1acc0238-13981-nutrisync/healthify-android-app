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
 * "Context" variants of the Figma offline banner (Banner/SinConexión).
 * - [Queueable]: what is logged is queued and sent later (diary, self weigh-in, off-plan).
 * - [RequiresNetwork]: the action needs network (sign-up, sign-in, practitioner actions).
 */
enum class OfflineBannerType(@param:StringRes val messageRes: Int) {
    Queueable(R.string.ds_offline_queueable),
    RequiresNetwork(R.string.ds_offline_requires_network),
}

/**
 * Persistent connectivity notice under the TopAppBar (replaces the Figma's offline Toast).
 * Announced to TalkBack when it appears.
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

@Preview(name = "Offline banner · Queueable / RequiresNetwork", widthDp = 360)
@Composable
private fun OfflineBannerPreview() {
    ComponentPreview {
        OfflineBannerType.entries.forEach { OfflineBanner(type = it) }
    }
}
