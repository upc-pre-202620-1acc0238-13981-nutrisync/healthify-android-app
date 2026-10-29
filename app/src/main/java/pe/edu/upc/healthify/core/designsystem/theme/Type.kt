package pe.edu.upc.healthify.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pe.edu.upc.healthify.R

private val Anton = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

private val OpenSans = FontFamily(
    Font(R.font.open_sans_regular, FontWeight.Normal),
    Font(R.font.open_sans_semibold, FontWeight.SemiBold),
    Font(R.font.open_sans_bold, FontWeight.Bold),
)

private fun anton(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = Anton,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

private fun openSans(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = OpenSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

private val DisplaySmall = anton(36, 44)
private val HeadlineMedium = anton(28, 36)

/**
 * Figma text styles (Display/Headline/Title/Body/Label): 12 here and 4 in
 * [HealthifyExtendedTypography] = the 16 styles. Anton for Display/Headline/Title Large; Open Sans for the rest.
 */
internal val HealthifyTypography = Typography(
    // The Figma does not define Display Large/Medium or Headline Large. They match the closest
    // Figma style so Material's default font never shows up.
    displayLarge = DisplaySmall,
    displayMedium = DisplaySmall,
    displaySmall = DisplaySmall,
    headlineLarge = HeadlineMedium,
    headlineMedium = HeadlineMedium,
    headlineSmall = anton(24, 32),
    titleLarge = anton(22, 28),
    titleMedium = openSans(FontWeight.SemiBold, 16, 24),
    titleSmall = openSans(FontWeight.SemiBold, 14, 20),
    bodyLarge = openSans(FontWeight.Normal, 16, 24),
    bodyMedium = openSans(FontWeight.Normal, 14, 20),
    bodySmall = openSans(FontWeight.Normal, 12, 16),
    labelLarge = openSans(FontWeight.SemiBold, 14, 20),
    labelMedium = openSans(FontWeight.SemiBold, 12, 16),
    labelSmall = openSans(FontWeight.SemiBold, 11, 16),
)

/** Figma styles that Material 3 does not have. */
@Immutable
data class HealthifyExtendedTypography(
    val buttonLarge: TextStyle,
    val overlineSection: TextStyle,
    val metricLarge: TextStyle,
    val metricMedium: TextStyle,
)

internal val DefaultExtendedTypography = HealthifyExtendedTypography(
    buttonLarge = openSans(FontWeight.Bold, 16, 24),
    // Figma: letter spacing 0.5 px.
    overlineSection = openSans(FontWeight.Bold, 12, 16).copy(letterSpacing = 0.5.sp),
    metricLarge = anton(32, 40),
    metricMedium = anton(22, 28),
)

internal val LocalExtendedTypography = staticCompositionLocalOf { DefaultExtendedTypography }
