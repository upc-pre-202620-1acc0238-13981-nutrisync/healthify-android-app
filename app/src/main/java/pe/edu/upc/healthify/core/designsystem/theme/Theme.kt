package pe.edu.upc.healthify.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Modes of the Figma "Color" collection. Only [Light] exists today.
 *
 * To add dark mode once the Figma defines it: create the `Dark` entry, its [ColorScheme] and its
 * [HealthifyExtendedColors], and resolve them in [colorSchemeFor] / [extendedColorsFor]. No screen
 * changes, because they all read the roles from the theme.
 */
enum class HealthifyColorMode { Light }

// Figma "Color" collection (Light mode): 25 M3 roles defined in the Figma + 3 derived ones
// (background, onBackground and surfaceVariant) that Material 3 still reads in some components.
private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Neutral0,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = Orange400,
    onSecondary = Neutral900,
    secondaryContainer = Orange50,
    onSecondaryContainer = Brown800,
    tertiary = Lime400,
    surface = Neutral50,
    surfaceContainerLowest = Neutral0,
    surfaceContainer = Neutral100,
    surfaceContainerHigh = Neutral200,
    onSurface = Neutral900,
    onSurfaceVariant = Neutral600,
    outline = Neutral500,
    outlineVariant = Neutral200,
    error = Red700,
    onError = Neutral0,
    errorContainer = Red50,
    onErrorContainer = Red900,
    inverseSurface = Neutral800,
    inverseOnSurface = Neutral50,
    inversePrimary = Lime200,
    scrim = Neutral1000,
    // Derived (same value as their Figma role).
    background = Neutral50,
    onBackground = Neutral900,
    surfaceVariant = Neutral100,
    // M3 roles the Figma does not define. They are pinned to the Healthify palette so
    // no Material component shows the baseline's default purples.
    onTertiary = Neutral900,
    tertiaryContainer = Green50,
    onTertiaryContainer = Green900,
    surfaceDim = Neutral100,
    surfaceBright = Neutral50,
    surfaceContainerLow = Neutral50,
    surfaceContainerHighest = Neutral200,
    surfaceTint = Green700,
)

private fun colorSchemeFor(mode: HealthifyColorMode): ColorScheme = when (mode) {
    HealthifyColorMode.Light -> LightColorScheme
}

private fun extendedColorsFor(mode: HealthifyColorMode): HealthifyExtendedColors = when (mode) {
    HealthifyColorMode.Light -> LightExtendedColors
}

private val DefaultDimens = HealthifyDimens()

private val HealthifyShapes = Shapes(
    extraSmall = RoundedCornerShape(DefaultDimens.radiusXs),
    small = RoundedCornerShape(DefaultDimens.radiusSm),
    medium = RoundedCornerShape(DefaultDimens.radiusMd),
    large = RoundedCornerShape(DefaultDimens.radiusLg),
    extraLarge = RoundedCornerShape(DefaultDimens.radiusXl),
)

/**
 * Healthify theme. The Figma only defines light mode: no dark scheme or dynamic color.
 * The app stays light until the Figma defines a dark mode
 * (see [HealthifyColorMode]).
 */
@Composable
fun HealthifyTheme(
    mode: HealthifyColorMode = HealthifyColorMode.Light,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColorsFor(mode),
        LocalExtendedTypography provides DefaultExtendedTypography,
        LocalDimens provides DefaultDimens,
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFor(mode),
            typography = HealthifyTypography,
            shapes = HealthifyShapes,
            content = content,
        )
    }
}

/** Access to the Figma tokens that are not part of Material 3. */
object HealthifyTheme {
    val extendedColors: HealthifyExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

    val extendedTypography: HealthifyExtendedTypography
        @Composable @ReadOnlyComposable get() = LocalExtendedTypography.current

    val dimens: HealthifyDimens
        @Composable @ReadOnlyComposable get() = LocalDimens.current
}
