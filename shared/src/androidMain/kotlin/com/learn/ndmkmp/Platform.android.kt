package com.learn.ndmkmp

import android.os.Build

// 🤖 actual for `expect fun platformName()` (commonMain/Platform.kt), read from the Android SDK.
actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"
