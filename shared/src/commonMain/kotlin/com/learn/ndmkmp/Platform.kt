package com.learn.ndmkmp

/** Name and version of the OS the app is running on, e.g. "Android 15" or "iOS 26.0". */
expect fun platformName(): String

/**
 * Milliseconds since boot, never jumps when the user changes the clock — for "seen 3 s ago" and scan intervals.
 * Android: `SystemClock.elapsedRealtime()` (same clock as `ScanResult.timestampNanos`). iOS: `NSProcessInfo.systemUptime`.
 */
expect fun monotonicMs(): Long

/** `HH:mm:ss.SSS` in the phone's time zone, for log lines (Received Data). Replaces JVM-only `SimpleDateFormat`. */
expect fun clockText(epochMs: Long): String

/** ISO 8601 local time with offset, e.g. `2026-10-05T09:30:00+07:00` — the `DeviceStatus.timestamp` format. */
expect fun isoLocalTimestamp(epochMs: Long): String
