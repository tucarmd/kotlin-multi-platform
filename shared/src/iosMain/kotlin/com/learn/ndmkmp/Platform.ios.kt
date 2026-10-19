package com.learn.ndmkmp

import platform.UIKit.UIDevice

// 🍎 actual for `expect fun platformName()`: Kotlin/Native calls UIKit directly (`import platform.UIKit.*`), no Swift.
actual fun platformName(): String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
