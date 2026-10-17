package com.nexvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nexvoice.app.ui.navigation.NexVoiceNavGraph
import com.nexvoice.app.ui.theme.NexVoiceTheme

/**
 * MainActivity.kt
 *
 * This is the ONE and only Activity in the app.
 * In modern Android with Jetpack Compose, you don't need multiple Activities.
 * Instead, Compose handles all the "screens" (called Composables) inside this single Activity.
 *
 * What it does:
 * 1. Installs the Splash Screen (system-level, works on Android 12+)
 * 2. Enables edge-to-edge display (content goes behind the status bar)
 * 3. Sets the Compose UI tree as the content
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate to intercept the splash
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Allows our app to draw behind the system bars (status bar / navigation bar)
        enableEdgeToEdge()

        setContent {
            // NexVoiceTheme applies our custom dark gaming color palette
            NexVoiceTheme {
                // NexVoiceNavGraph manages all screen transitions
                NexVoiceNavGraph()
            }
        }
    }
}
