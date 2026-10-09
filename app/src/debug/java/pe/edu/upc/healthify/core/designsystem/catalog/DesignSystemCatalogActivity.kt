package pe.edu.upc.healthify.core.designsystem.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme

/**
 * Catálogo interno del sistema de diseño (solo debug): todos los componentes de `core/designsystem` con
 * sus estados, para revisarlos contra el Figma «Sistema de diseño · Healthify M3».
 * Tiene su propio ícono en el launcher («Healthify · Catálogo DS»).
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
