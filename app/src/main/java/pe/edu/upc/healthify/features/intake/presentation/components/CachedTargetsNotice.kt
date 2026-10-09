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
 * «Grupo · aviso» de PT3.O: banner «Sin conexión» (lo registrado se encola) y, si las metas vienen de la copia del
 * teléfono, desde cuándo («Metas guardadas en tu teléfono · actualizadas hoy 8:10 a. m.»).
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

@Preview(name = "PT3.O · Grupo aviso (metas desde caché)", widthDp = 360)
@Composable
private fun CachedTargetsNoticePreview() {
    HealthifyTheme { CachedTargetsNotice(cachedAt = Instant.now()) }
}
