package pe.edu.upc.healthify.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Roles de color del Figma (colección «Color») que Material 3 no tiene. */
@Immutable
data class HealthifyExtendedColors(
    val brandDecor: Color,
    val brandDecorLight: Color,
    val disabledContainer: Color,
    val onDisabled: Color,
    val secondaryPressed: Color,
    val primaryContainerPressed: Color,
    val primaryOverlayPressed: Color,
    val errorOverlayPressed: Color,
    val cardPressed: Color,
)

internal val LightExtendedColors = HealthifyExtendedColors(
    brandDecor = Green500,
    brandDecorLight = Green50,
    disabledContainer = StateDisabledContainer,
    onDisabled = StateDisabledContent,
    secondaryPressed = StateSecondaryPressed,
    primaryContainerPressed = StatePrimaryContainerPressed,
    primaryOverlayPressed = StatePrimaryOverlayPressed,
    errorOverlayPressed = StateErrorOverlayPressed,
    cardPressed = StateCardPressed,
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
