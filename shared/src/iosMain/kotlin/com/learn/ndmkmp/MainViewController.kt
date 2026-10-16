package com.learn.ndmkmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo

// `xcrun simctl launch booted com.learn.ndmkmp -tab monitor` opens a tab directly (screenshots), like `--es tab` on Android.
fun MainViewController() = ComposeUIViewController { App(startTab = launchArgument("-tab")) }

private fun launchArgument(name: String): String? {
    val args = NSProcessInfo.processInfo.arguments.map { it.toString() }
    return args.getOrNull(args.indexOf(name) + 1)?.takeIf { name in args }
}
