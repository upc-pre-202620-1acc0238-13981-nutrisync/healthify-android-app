package pe.edu.upc.healthify.core.designsystem.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Debug-only catalog of every `core/designsystem` component and its states, used to check them
 * against the Figma design system. It has no launcher icon; open it with
 * `adb shell am start -n pe.edu.upc.healthify/.core.designsystem.catalog.DesignSystemCatalogActivity`.
 */
class DesignSystemCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthifyTheme {
                DesignSystemCatalogScreen(onBack = ::finish)
            }
        }
    }
}
