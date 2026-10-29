package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyCard
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

// Measurements from the "Something doesn't add up" frame: 37 dp help icon.
private val HelpIconSize = 37.dp

/**
 * Something doesn't add up. An open question, never an accusation (*Invitation Tone Never Accusation*), and it
 * happens **before** any escalation. Static content: the acknowledgement was already recorded when the home card was
 * drawn, and the home card only links here with an active alert.
 */
@Composable
fun ConsistencyNoticeScreen(
    onBack: () -> Unit,
    onReviewDiary: () -> Unit,
    onWhatItMeans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    StaticScreen(title = stringResource(R.string.consistency_title), onBack = onBack, modifier = modifier) {
        HealthifyCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = HealthifyIcons.Help,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(HelpIconSize),
                )
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
                    Text(
                        text = stringResource(R.string.consistency_title),
                        style = HealthifyTheme.extendedTypography.metricMedium,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.consistency_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                    HealthifyButton(
                        text = stringResource(R.string.consistency_review_diary),
                        onClick = onReviewDiary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HealthifyButton(
                        text = stringResource(R.string.consistency_what_it_means),
                        onClick = onWhatItMeans,
                        style = HealthifyButtonStyle.Text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * What happens if this keeps up. Makes escalation explicit (never a surprise) and makes clear that a signal
 * notifies but does not change the plan (*Signal Notifies Never Modifies The Plan*). Static, also offline.
 */
@Composable
fun ConsistencyEscalationInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    StaticScreen(title = stringResource(R.string.consistency_escalation_title), onBack = onBack, modifier = modifier) {
        HealthifyCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
            ) {
                Text(
                    text = stringResource(R.string.consistency_escalation_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.consistency_escalation_note),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.primary,
                )
            }
        }
        HealthifyButton(
            text = stringResource(R.string.consistency_escalation_ok),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StaticScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
            content = content,
        )
    }
}

@Preview(name = "Something doesn't add up", widthDp = 360, heightDp = 800)
@Composable
private fun ConsistencyNoticePreview() {
    HealthifyTheme { ConsistencyNoticeScreen(onBack = {}, onReviewDiary = {}, onWhatItMeans = {}) }
}

@Preview(name = "What happens if this keeps up", widthDp = 360, heightDp = 800)
@Composable
private fun ConsistencyEscalationInfoPreview() {
    HealthifyTheme { ConsistencyEscalationInfoScreen(onBack = {}) }
}
