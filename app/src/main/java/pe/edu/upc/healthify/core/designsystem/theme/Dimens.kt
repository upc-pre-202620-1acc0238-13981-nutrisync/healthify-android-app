package pe.edu.upc.healthify.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Figma "Dimensiones" collection (spacing 0–64 in multiples of 4/8, radii and sizes) plus
 * elevations 1–3 ("Elevation/…" effect styles).
 */
@Immutable
data class HealthifyDimens(
    val space0: Dp = 0.dp,
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space40: Dp = 40.dp,
    val space48: Dp = 48.dp,
    val space64: Dp = 64.dp,
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 24.dp,
    /** "radius/full" (999 in the Figma): pill or circle shape. */
    val radiusFull: Dp = 999.dp,
    // Component radii that are not Figma variables but are fixed values of its components.
    /** Snackbar. */
    val radiusXs: Dp = 4.dp,
    /** Skeleton text blocks. */
    val radiusSkeletonText: Dp = 6.dp,
    /** Dialog and BottomSheet (M3). */
    val radiusDialog: Dp = 28.dp,
    /** Resting border (Outlined, TextField, Segment). */
    val borderThin: Dp = 1.dp,
    /** Focus/selection/error border. */
    val borderThick: Dp = 2.dp,
    val touchMin: Dp = 48.dp,
    val icon: Dp = 24.dp,
    val iconSmall: Dp = 16.dp,
    val statusBar: Dp = 24.dp,
    val gestureBar: Dp = 24.dp,
    val button: Dp = 56.dp,
    val textField: Dp = 56.dp,
    /** Side screen margin in the 360×800 reference frame. */
    val screenHorizontal: Dp = 16.dp,
    val elevation1: HealthifyElevation = HealthifyElevation.Level1,
    val elevation2: HealthifyElevation = HealthifyElevation.Level2,
    val elevation3: HealthifyElevation = HealthifyElevation.Level3,
)

internal val LocalDimens = staticCompositionLocalOf { HealthifyDimens() }
