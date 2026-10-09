package pe.edu.upc.healthify.features.iam.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.presentation.state.ShellDestination
import pe.edu.upc.healthify.features.iam.presentation.state.ShellLoadingEvent
import pe.edu.upc.healthify.features.iam.presentation.state.ShellLoadingUiState
import pe.edu.upc.healthify.features.iam.presentation.viewmodel.ShellLoadingViewModel

// Medidas del frame «S5 · Cargando — Normal».
private val LogoSize = DpSize(72.dp, 63.dp)
private val LoaderSize = 80.dp

// «Grupo fijo»: tres arcos (`arcData` del Figma, radio interior 0.62), en grados desde las 3 en punto.
private const val INNER_RADIUS_RATIO = 0.62f
private val LoaderArcs = listOf(-90f to 34.4f, 51.6f to 149f, 166.2f to 263.6f)
private const val LOADER_TURN_MS = 1_400
private const val FULL_TURN = 360f

/** S5 · Cargando (Normal / Error). Pa y Nu comparten el visual; el destino depende del shell y del vínculo. */
@Composable
fun ShellLoadingScreen(
    onNavigate: (ShellDestination) -> Unit,
    onNoSession: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShellLoadingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is ShellLoadingEvent.Navigate -> onNavigate(event.destination)
            ShellLoadingEvent.NavigateToWelcome -> onNoSession()
        }
    }
    ShellLoadingContent(state = state, onRetry = viewModel::onRetry, onClose = onClose, modifier = modifier)
}

/**
 * @param onClose DECISIÓN S5: «Cerrar» del estado de error cierra la app (la sesión se conserva para el próximo
 *   intento); no cierra la sesión.
 */
@Composable
fun ShellLoadingContent(
    state: ShellLoadingUiState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.space16, vertical = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space24, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isError) {
            ErrorState(
                onRetry = onRetry,
                retrying = state.isRetrying,
                secondaryLabel = stringResource(R.string.common_close),
                onSecondary = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            HealthifyLogo(variant = HealthifyLogoVariant.Color, size = LogoSize, decorative = true)
            BrandLoader()
            Text(
                text = stringResource(R.string.shell_loading_text),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * Indicador de carga de la marca (tres arcos `secondary`, `tertiary`, `primary`).
 * DECISIÓN S5: el frame es estático; en la app gira para indicar que la carga sigue en curso.
 */
@Composable
private fun BrandLoader(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = listOf(scheme.secondary, scheme.tertiary, scheme.primary)
    val rotation by rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(LOADER_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "loaderRotation",
    )
    Canvas(
        modifier = modifier
            .size(LoaderSize)
            .rotate(rotation)
            .clearAndSetSemantics {},
    ) {
        val outer = size.minDimension / 2
        val thickness = outer * (1 - INNER_RADIUS_RATIO)
        val radius = outer - thickness / 2
        val topLeft = Offset(center.x - radius, center.y - radius)
        LoaderArcs.forEachIndexed { index, (start, end) ->
            drawArc(
                color = colors[index],
                startAngle = start,
                sweepAngle = end - start,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = thickness),
            )
        }
    }
}

@Preview(name = "S5 · Cargando — Normal", widthDp = 360, heightDp = 800)
@Composable
private fun ShellLoadingPreview() {
    HealthifyTheme { ShellLoadingContent(state = ShellLoadingUiState(), onRetry = {}, onClose = {}) }
}

@Preview(name = "S5 · Cargando — Error", widthDp = 360, heightDp = 800)
@Composable
private fun ShellLoadingErrorPreview() {
    HealthifyTheme { ShellLoadingContent(state = ShellLoadingUiState(isError = true), onRetry = {}, onClose = {}) }
}
