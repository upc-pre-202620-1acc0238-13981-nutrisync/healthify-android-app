package pe.edu.upc.healthify.features.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.isToday
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.component.shortTimeText
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import java.time.Instant

/**
 * The offline notice group: an "Offline" banner (logs get queued) and, if the targets come from the phone's
 * copy, since when ("Targets saved on your phone · updated today 8:10 a.m.").
 */
@Composable
fun CachedTargetsNotice(cachedAt: Instant?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space4)) {
        OfflineBanner(type = OfflineBannerType.Queueable)
        if (cachedAt != null) {
            val text = if (cachedAt.isToday()) {
                stringResource(R.string.home_cached_today, cachedAt.shortTimeText())
            } else {
                stringResource(
                    R.string.home_cached_on,
                    stringResource(R.string.format_separator_dot, cachedAt.mediumDateText(), cachedAt.shortTimeText()),
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "Notice group (targets from cache)", widthDp = 360)
@Composable
private fun CachedTargetsNoticePreview() {
    HealthifyTheme { CachedTargetsNotice(cachedAt = Instant.now()) }
}
