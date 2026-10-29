package pe.edu.upc.healthify.core.designsystem.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Status bar and navigation bar icon color based on the screen background (the Figma "StatusBar/Android"
 * light and dark icon variants). With edge-to-edge the app draws behind the bars; leaving the screen
 * restores the previous value.
 *
 * @param darkBackground `true` = dark or strongly colored background → light icons.
 */
@Composable
fun SystemBarsAppearance(darkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, darkBackground) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !darkBackground
        controller?.isAppearanceLightNavigationBars = !darkBackground
        onDispose {
            if (controller != null && previousStatus != null && previousNavigation != null) {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
