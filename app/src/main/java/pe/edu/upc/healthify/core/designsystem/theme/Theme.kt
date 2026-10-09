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
 * Modos de la colección «Color» del Figma. Hoy solo existe [Light].
 *
 * Para agregar el modo oscuro cuando el Figma lo defina: crear la entrada `Dark`, su [ColorScheme] y sus
 * [HealthifyExtendedColors], y resolverlos en [colorSchemeFor] / [extendedColorsFor]. Ninguna pantalla
 * cambia, porque todas leen los roles desde el tema.
 */
enum class HealthifyColorMode { Light }

// Colección «Color» del Figma (modo Light): 25 roles M3 definidos en el Figma + 3 derivados
// (background, onBackground y surfaceVariant) que Material 3 todavía lee en algunos componentes.
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
    // Derivados (mismo valor que su rol del Figma).
    background = Neutral50,
    onBackground = Neutral900,
    surfaceVariant = Neutral100,
    // DECISIÓN Sistema de diseño: roles M3 que el Figma no define. Se fijan a la paleta de Healthify para
    // que ningún componente de Material muestre los morados por defecto del baseline.
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
 * Tema de Healthify. El Figma solo define modo claro: no hay esquema oscuro ni color dinámico.
 * DECISIÓN Sistema de diseño: la app es siempre clara hasta que el Figma defina un modo oscuro
 * (ver [HealthifyColorMode]).
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

/** Acceso a los tokens del Figma que no son de Material 3. */
object HealthifyTheme {
    val extendedColors: HealthifyExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

    val extendedTypography: HealthifyExtendedTypography
        @Composable @ReadOnlyComposable get() = LocalExtendedTypography.current

    val dimens: HealthifyDimens
        @Composable @ReadOnlyComposable get() = LocalDimens.current
}
