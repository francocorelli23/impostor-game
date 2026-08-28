package com.impostor.party

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.impostor.party.ui.ImpostorApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the cold-start theme hands over cleanly.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ImpostorApp()
        }
    }

    override fun onPause() {
        super.onPause()
        (application as? ImpostorApplication)?.container?.sound?.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        (application as? ImpostorApplication)?.container?.sound?.resumeMusic()
    }

    override fun onDestroy() {
        if (isFinishing) {
            (application as? ImpostorApplication)?.container?.sound?.release()
        }
        super.onDestroy()
    }
}
