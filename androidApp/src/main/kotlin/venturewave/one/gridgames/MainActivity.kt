package venturewave.one.gridgames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import venturewave.one.gridgames.data.storage.FileStorageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize file storage for pattern persistence
        FileStorageProvider.initialize(this)

        setContent {
            App()
        }
    }
}