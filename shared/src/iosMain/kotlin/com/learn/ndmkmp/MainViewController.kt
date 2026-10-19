package com.learn.ndmkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.learn.ndmkmp.platform.iosServices
import com.learn.ndmkmp.presentation.AppViewModel
import platform.Foundation.NSProcessInfo

/** R2: no longer used by the app (SwiftUI draws the tabs) — removed with Compose MP in R4. */
// `xcrun simctl launch booted com.learn.ndmkmp -tab monitor` opens a tab directly (screenshots), like `--es tab` on Android.
fun MainViewController() = ComposeUIViewController {
    App(androidx.compose.runtime.remember { AppViewModel(iosServices, startTab = launchArgument("-tab")) })
}

private fun launchArgument(name: String): String? {
    val args = NSProcessInfo.processInfo.arguments.map { it.toString() }
    return args.getOrNull(args.indexOf(name) + 1)?.takeIf { name in args }
}
