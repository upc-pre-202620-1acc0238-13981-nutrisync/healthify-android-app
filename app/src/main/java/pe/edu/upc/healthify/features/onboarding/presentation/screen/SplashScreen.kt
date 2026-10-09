package pe.edu.upc.healthify.features.onboarding.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashEvent
import pe.edu.upc.healthify.features.onboarding.presentation.viewmodel.SplashViewModel

// Medidas del frame «S1 · Splash».
private val TopCircleSize = 320.dp
private val TopCircleOffsetX = 120.dp
private val TopCircleOffsetY = (-70).dp
private val BottomCircleSize = 300.dp
private val BottomCircleOffsetX = (-140).dp
private val BottomCircleOffsetY = 50.dp
private val HaloSize = 180.dp
private val LogoSize = DpSize(154.dp, 126.dp)
private val LogoTop = 20.dp
private val NameTop = 148.dp
private val DotSize = 6.dp
private val DotGap = 9.dp
private const val DECOR_ALPHA = 0.3f
private const val HALO_ALPHA = 0.14f
private const val DOT_COUNT = 3

/** S1 · Splash. Navega sola en cuanto sabe si hay sesión guardada. */
@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is SplashEvent.Navigate -> onNavigate(event.destination)
        }
    }
    SplashContent(modifier = modifier)
}

@Composable
fun SplashContent(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val loadingDescription = stringResource(R.string.ds_cd_loading)
    SystemBarsAppearance(darkBackground = false)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.secondary)
            .semantics { contentDescription = loadingDescription },
    ) {
        DecorCircle(
            size = TopCircleSize,
            color = scheme.tertiary.copy(alpha = DECOR_ALPHA),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = TopCircleOffsetX, y = TopCircleOffsetY),
        )
        DecorCircle(
            size = BottomCircleSize,
            color = scheme.primary.copy(alpha = DECOR_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = BottomCircleOffsetX, y = BottomCircleOffsetY),
        )
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.TopCenter) {
            DecorCircle(size = HaloSize, color = scheme.surfaceContainerLowest.copy(alpha = HALO_ALPHA))
            HealthifyLogo(
                variant = HealthifyLogoVariant.White,
                size = LogoSize,
                decorative = true,
                modifier = Modifier.offset(y = LogoTop),
            )
            Column(
                modifier = Modifier.offset(y = NameTop),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(HealthifyTheme.dimens.space12))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DotGap),
                    modifier = Modifier.clearAndSetSemantics {},
                ) {
                    repeat(DOT_COUNT) { DecorCircle(size = DotSize, color = scheme.onSecondary) }
                }
            }
        }
    }
}

@Composable
private fun DecorCircle(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
    )
}

@Preview(name = "S1 · Splash", widthDp = 360, heightDp = 800)
@Composable
private fun SplashContentPreview() {
    HealthifyTheme { SplashContent() }
}
