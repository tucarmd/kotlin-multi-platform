package dev.tipsplit.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.tipsplit.android.ui.SplitRoute

/**
 * [App 1 · B5] The only Activity. It just hands the window to Compose.
 *
 * [Bài 6 · Một App(), hai điểm khởi động] With Compose Multiplatform, Android starts the shared UI here with
 * `setContent { App() }` and iOS from MainViewController(). Tip Split only shares the logic, so Android starts
 * its own SplitRoute() here and iOS starts SwiftUI in iOSApp.swift.
 *
 * [Bài 8 · Điều hướng] One screen, so no NavHost. Later apps put NavHost in App.kt and call it from here.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SplitRoute() }
    }
}
