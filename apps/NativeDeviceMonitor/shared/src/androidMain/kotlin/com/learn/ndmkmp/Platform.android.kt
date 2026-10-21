package com.learn.ndmkmp

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE}"

actual fun monotonicMs(): Long = android.os.SystemClock.elapsedRealtime()

// SimpleDateFormat is not thread-safe → a new one per call (cheap next to drawing a log line).
actual fun clockText(epochMs: Long): String =
    java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(epochMs))

actual fun isoLocalTimestamp(epochMs: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date(epochMs))
