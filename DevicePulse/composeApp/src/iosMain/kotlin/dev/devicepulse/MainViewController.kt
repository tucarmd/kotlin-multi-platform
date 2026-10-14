package dev.devicepulse

import androidx.compose.ui.window.ComposeUIViewController
import dev.devicepulse.di.initKoin
import dev.devicepulse.ui.App
import platform.UIKit.UIViewController

private var koinStarted = false

/** Entry point called from Swift (`MainViewControllerKt.MainViewController()`). */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
