package com.learn.ndmkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.learn.ndmkmp.platform.createIosServices
import platform.Foundation.NSProcessInfo

/** One per process: the BG task handler (P7) and the screen must share the same monitor / logger. */
val iosServices: AppServices by lazy { createIosServices() }

// `xcrun simctl launch booted com.learn.ndmkmp -tab monitor` opens a tab directly (screenshots), like `--es tab` on Android.
fun MainViewController() = ComposeUIViewController { App(startTab = launchArgument("-tab"), services = iosServices) }

private fun launchArgument(name: String): String? {
    val args = NSProcessInfo.processInfo.arguments.map { it.toString() }
    return args.getOrNull(args.indexOf(name) + 1)?.takeIf { name in args }
}
