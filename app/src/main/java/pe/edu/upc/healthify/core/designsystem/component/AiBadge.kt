package pe.edu.upc.healthify.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Figma AI badge, as on the diary's AI ideas and the weight trend's AI summaries. Everything AI-generated
 * carries this mark.
 */
@Composable
fun AiBadge(label: String, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(scheme.primaryContainer)
            .padding(horizontal = dimens.space8, vertical = dimens.space4),
        horizontalArrangement = Arrangement.spacedBy(dimens.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = HealthifyIcons.Ai,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(dimens.iconSmall),
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer)
    }
}

@Preview(name = "Badge · AI")
@Composable
private fun AiBadgePreview() {
    ComponentPreview { AiBadge(label = "Resumen con IA") }
}
