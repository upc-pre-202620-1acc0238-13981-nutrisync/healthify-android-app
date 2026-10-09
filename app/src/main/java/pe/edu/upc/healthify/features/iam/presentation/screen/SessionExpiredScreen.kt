package pe.edu.upc.healthify.features.iam.presentation.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import pe.edu.upc.healthify.features.iam.presentation.state.SessionExpiredUiState
import pe.edu.upc.healthify.features.iam.presentation.viewmodel.SessionExpiredViewModel

// Medidas de los frames «S6 · Sesión expirada» (Normal / Sin conexión).
private val LogoSize = DpSize(64.dp, 57.dp)
private val BadgeSizeNormal = 80.dp
private val BadgeSizeOffline = 88.dp
private val LockBodySize = DpSize(34.dp, 26.dp)
private val LockBodyRadius = 6.dp
private val ShackleRadius = 14.dp
private val ShackleStroke = 5.dp
private val ShackleBottomOverlap = 2.dp

/** S6 · Sesión expirada (Normal / Sin conexión). Tono neutro: nunca dice por qué venció (regla ética). */
@Composable
fun SessionExpiredScreen(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionExpiredViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionExpiredContent(state = state, onSignIn = onSignIn, modifier = modifier)
}

@Composable
fun SessionExpiredContent(
    state: SessionExpiredUiState,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val offline = state.isOffline
    SystemBarsAppearance(darkBackground = !offline)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (offline) scheme.surface else scheme.inverseSurface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.space16, vertical = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space24, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (offline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        HealthifyLogo(
            variant = if (offline) HealthifyLogoVariant.Color else HealthifyLogoVariant.White,
            size = LogoSize,
            decorative = true,
        )
        if (offline) {
            LockBadge(
                size = BadgeSizeOffline,
                container = scheme.surfaceContainerLowest,
                lock = scheme.primary,
                elevated = true,
            )
        } else {
            LockBadge(size = BadgeSizeNormal, container = scheme.onSurface, lock = scheme.secondary, elevated = false)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.session_expired_title),
                style = if (offline) HealthifyTheme.extendedTypography.metricMedium else MaterialTheme.typography.headlineSmall,
                color = if (offline) scheme.onSurface else scheme.inverseOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.session_expired_body),
                style = MaterialTheme.typography.bodyMedium,
                color = if (offline) scheme.onSurfaceVariant else scheme.inverseOnSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (offline) {
            Text(
                text = stringResource(R.string.session_expired_offline),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HealthifyButton(
            text = stringResource(R.string.sign_in_submit),
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Candado de S6 dibujado como en el Figma (círculo, arco y cuerpo redondeado). */
@Composable
private fun LockBadge(size: Dp, container: Color, lock: Color, elevated: Boolean, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    Box(
        modifier = modifier
            .size(size)
            .elevation(if (elevated) dimens.elevation1 else null, CircleShape)
            .background(container, CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val bodyWidth = LockBodySize.width.toPx()
            val bodyHeight = LockBodySize.height.toPx()
            val shackleRadius = ShackleRadius.toPx()
            val bodyTop = center.y - bodyHeight / 2 + shackleRadius / 2
            val shackleCenterY = bodyTop + ShackleBottomOverlap.toPx()
            drawArc(
                color = lock,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - shackleRadius, shackleCenterY - shackleRadius),
                size = Size(shackleRadius * 2, shackleRadius * 2),
                style = Stroke(width = ShackleStroke.toPx(), cap = StrokeCap.Round),
            )
            drawRoundRect(
                color = lock,
                topLeft = Offset(center.x - bodyWidth / 2, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(LockBodyRadius.toPx()),
            )
        }
    }
}

@Preview(name = "S6 · Sesión expirada — Normal", widthDp = 360, heightDp = 800)
@Composable
private fun SessionExpiredPreview() {
    HealthifyTheme { SessionExpiredContent(state = SessionExpiredUiState(), onSignIn = {}) }
}

@Preview(name = "S6 · Sesión expirada — Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun SessionExpiredOfflinePreview() {
    HealthifyTheme { SessionExpiredContent(state = SessionExpiredUiState(isOffline = true), onSignIn = {}) }
}
