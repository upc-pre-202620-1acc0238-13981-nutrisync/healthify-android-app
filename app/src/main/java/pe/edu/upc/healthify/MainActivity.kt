package pe.edu.upc.healthify

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.network.AndroidAppLanguageSetter
import pe.edu.upc.healthify.navigation.AppNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Before Android 13 the language chosen in the app is applied here (after that the system does it).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidAppLanguageSetter.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            HealthifyTheme {
                AppNavHost(navController = navController)
            }
        }
    }
}
