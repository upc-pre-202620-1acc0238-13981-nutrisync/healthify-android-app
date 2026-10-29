package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Measurements from the consent withdrawn frame.
private val CheckBadgeSize = 84.dp

/**
 * Consent withdrawn. Static and available offline (the action already finished). The tone is of a legitimate,
 * settled decision, with no loss or blame, leaving the door open to link with someone else.
 */
@Composable
fun ConsentWithdrawnScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    // Back does not return to the withdraw screen: the care link is already closed.
    BackHandler(onBack = onDone)
    ConsentWithdrawnContent(onDone = onDone, modifier = modifier)
}

@Composable
fun ConsentWithdrawnContent(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.space16, vertical = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(CheckBadgeSize)
                    .clip(CircleShape)
                    .background(scheme.onSurface)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.consent_withdrawn_glyph),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.inversePrimary,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { heading() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.consent_withdrawn_title_top),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.inverseOnSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.consent_withdrawn_title_bottom),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.inversePrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(scheme.onSurface)
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space4),
            ) {
                Text(
                    text = stringResource(R.string.consent_withdrawn_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.inverseOnSurface,
                )
                Text(
                    text = stringResource(R.string.consent_withdrawn_next),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.inverseOnSurface,
                )
            }
        }
        HealthifyButton(
            text = stringResource(R.string.consent_withdrawn_done),
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
        )
    }
}

@Preview(name = "Consent withdrawn", widthDp = 360, heightDp = 800)
@Composable
private fun ConsentWithdrawnPreview() {
    HealthifyTheme { ConsentWithdrawnContent(onDone = {}) }
}
