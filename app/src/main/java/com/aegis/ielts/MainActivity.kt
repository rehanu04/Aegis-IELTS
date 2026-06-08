package com.aegis.ielts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aegis.ielts.navigation.AegisNavHost
import com.aegis.ielts.ui.theme.AegisIELTSTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity entry point for the Aegis IELTS application.
 *
 * Hosts [AegisNavHost] as the sole composable root, wrapped in [AegisIELTSTheme].
 * Navigation between all 4 modules (Speaking, Reading, Listening, Writing) is
 * handled internally by the NavHost; no inter-Activity navigation is used.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AegisIELTSTheme {
                AegisNavHost()
            }
        }
    }
}