package pe.edu.upc.healthify.features.onboarding.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.onboarding.presentation.components.WelcomeChip
import pe.edu.upc.healthify.features.onboarding.presentation.components.WelcomeChipIcon
import pe.edu.upc.healthify.features.onboarding.presentation.state.WelcomeEvent
import pe.edu.upc.healthify.features.onboarding.presentation.state.WelcomeUiState
import pe.edu.upc.healthify.features.onboarding.presentation.viewmodel.WelcomeViewModel

// Medidas del frame «S2 · Bienvenida — Normal», relativas al borde inferior de la status bar.
private val HeroHeight = 406.dp
private val LogoSize = DpSize(48.dp, 43.dp)
private val LogoPosition = DpOffset(20.dp, 10.dp)
private val GoalChipPosition = DpOffset(26.dp, 96.dp)
private val MealChipPosition = DpOffset(124.dp, 198.dp)
private val PractitionerChipPosition = DpOffset(34.dp, 294.dp)
private val LargeCircle = DecorSpec(DpOffset(150.dp, 26.dp), 290.dp)
private val MediumCircle = DecorSpec(DpOffset((-60).dp, 206.dp), 180.dp)
private val SmallCircle = DecorSpec(DpOffset(250.dp, 276.dp), 120.dp)
private const val DECOR_ALPHA = 0.82f
private const val GOAL_CHIP_ROTATION = -6f
private const val MEAL_CHIP_ROTATION = 5f
private const val PRACTITIONER_CHIP_ROTATION = -3f

private data class DpOffset(val x: Dp, val y: Dp)
private data class DecorSpec(val position: DpOffset, val size: Dp)

/** S2 · Bienvenida (Normal / Sin conexión). */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onHaveAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            WelcomeEvent.NavigateToSignUp -> onCreateAccount()
            WelcomeEvent.NavigateToSignIn -> onHaveAccount()
        }
    }
    WelcomeContent(
        state = state,
        onCreateAccount = viewModel::onCreateAccount,
        onHaveAccount = viewModel::onHaveAccount,
        modifier = modifier,
    )
}

@Composable
fun WelcomeContent(
    state: WelcomeUiState,
    onCreateAccount: () -> Unit,
    onHaveAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.showOfflineBanner) {
                OfflineBanner(
                    type = OfflineBannerType.RequiresNetwork,
                    modifier = Modifier.padding(start = dimens.space16, top = dimens.space16, end = dimens.space16),
                )
            }
            WelcomeHero()
            Column(
                modifier = Modifier.padding(horizontal = dimens.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(dimens.space24),
            ) {
                Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
                    Text(
                        text = stringResource(R.string.welcome_headline_first),
                        style = MaterialTheme.typography.displaySmall,
                        color = scheme.inverseOnSurface,
                    )
                    Text(
                        text = stringResource(R.string.welcome_headline_second),
                        style = MaterialTheme.typography.displaySmall,
                        color = scheme.inversePrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.welcome_body),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.inverseOnSurface,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = stringResource(R.string.welcome_create_account),
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.welcome_have_account),
                onClick = onHaveAccount,
                style = HealthifyButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
                contentColor = scheme.inversePrimary,
            )
        }
    }
}

/** Logo, círculos decorativos y las tres tarjetas ilustrativas (sin datos reales). */
@Composable
private fun WelcomeHero(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroHeight),
    ) {
        DecorCircle(LargeCircle, scheme.secondary)
        DecorCircle(MediumCircle, scheme.tertiary)
        DecorCircle(SmallCircle, HealthifyTheme.extendedColors.brandDecor)
        HealthifyLogo(
            variant = HealthifyLogoVariant.White,
            size = LogoSize,
            modifier = Modifier.offset(LogoPosition.x, LogoPosition.y),
        )
        WelcomeChip(
            label = stringResource(R.string.welcome_chip_goal_label),
            value = stringResource(R.string.welcome_chip_goal_value),
            icon = WelcomeChipIcon.Target,
            badgeColor = scheme.secondary,
            iconColor = scheme.surfaceContainerLowest,
            rotation = GOAL_CHIP_ROTATION,
            modifier = Modifier.offset(GoalChipPosition.x, GoalChipPosition.y),
        )
        WelcomeChip(
            label = stringResource(R.string.welcome_chip_meal_label),
            value = stringResource(R.string.welcome_chip_meal_value),
            icon = WelcomeChipIcon.Vector(HealthifyIcons.Camera),
            badgeColor = scheme.primary,
            iconColor = scheme.surfaceContainerLowest,
            rotation = MEAL_CHIP_ROTATION,
            modifier = Modifier.offset(MealChipPosition.x, MealChipPosition.y),
        )
        WelcomeChip(
            label = stringResource(R.string.welcome_chip_practitioner_label),
            value = stringResource(R.string.welcome_chip_practitioner_value),
            icon = WelcomeChipIcon.Vector(HealthifyIcons.Check),
            badgeColor = scheme.tertiary,
            iconColor = scheme.onSurface,
            rotation = PRACTITIONER_CHIP_ROTATION,
            modifier = Modifier.offset(PractitionerChipPosition.x, PractitionerChipPosition.y),
        )
    }
}

@Composable
private fun DecorCircle(spec: DecorSpec, color: Color) {
    Box(
        modifier = Modifier
            .offset(spec.position.x, spec.position.y)
            .size(spec.size)
            .alpha(DECOR_ALPHA)
            .background(color, CircleShape),
    )
}

@Preview(name = "S2 · Bienvenida — Normal", widthDp = 360, heightDp = 800)
@Composable
private fun WelcomeContentPreview() {
    HealthifyTheme { WelcomeContent(state = WelcomeUiState(), onCreateAccount = {}, onHaveAccount = {}) }
}

@Preview(name = "S2 · Bienvenida — Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun WelcomeContentOfflinePreview() {
    HealthifyTheme {
        WelcomeContent(state = WelcomeUiState(showOfflineBanner = true), onCreateAccount = {}, onHaveAccount = {})
    }
}
