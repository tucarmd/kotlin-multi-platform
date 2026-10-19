package com.learn.ndmkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * 📱 Android shell: the launcher screen (AndroidManifest.xml → intent-filter MAIN + LAUNCHER).
 * iOS counterpart: `iosApp/iosApp/iOSApp.swift` + `ContentView.swift`.
 *
 * ```
 *  MainActivity.onCreate
 *   ├─ enableEdgeToEdge()      draw behind status / nav bar; App() pads with safeDrawingPadding()
 *   └─ setContent { App() }    App() lives in shared/commonMain (same package, no import),
 *                              reachable through implementation(projects.shared) in androidApp/build.gradle.kts
 * ```
 * ComponentActivity / setContent / enableEdgeToEdge come from androidx.activity:activity-compose (libs.versions.toml).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before setContent so the first frame is already edge-to-edge.
        enableEdgeToEdge()
        // Compose replaces setContentView(R.layout.…): no XML layout in this app.
        setContent {
            App()
        }
    }
}
