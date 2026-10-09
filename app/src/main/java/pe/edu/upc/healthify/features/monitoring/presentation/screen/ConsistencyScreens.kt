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

// Medidas del frame «PT16 · Algo no cuadra»: ícono de ayuda de 37 dp.
private val HelpIconSize = 37.dp

/**
 * PT16 · Algo no cuadra. Pregunta abierta, nunca acusación (*Invitation Tone Never Accusation*), y ocurre **antes**
 * de cualquier escalación. Contenido estático: el acuse de MA-7 ya se registró al pintar la tarjeta en PT3, y desde
 * PT3 solo se llega con alerta activa.
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
 * PT17 · Qué pasa si esto sigue así. Hace explícita la escalación (nunca sorpresa) y deja claro que una señal
 * notifica pero no cambia el plan (*Signal Notifies Never Modifies The Plan*). Estático, también sin conexión.
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

@Preview(name = "PT16 · Algo no cuadra", widthDp = 360, heightDp = 800)
@Composable
private fun ConsistencyNoticePreview() {
    HealthifyTheme { ConsistencyNoticeScreen(onBack = {}, onReviewDiary = {}, onWhatItMeans = {}) }
}

@Preview(name = "PT17 · Qué pasa si esto sigue así", widthDp = 360, heightDp = 800)
@Composable
private fun ConsistencyEscalationInfoPreview() {
    HealthifyTheme { ConsistencyEscalationInfoScreen(onBack = {}) }
}
