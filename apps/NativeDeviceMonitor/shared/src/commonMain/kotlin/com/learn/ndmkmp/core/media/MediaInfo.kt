package com.learn.ndmkmp.core.media

/** Where the image came from — shown in the UI + lets the Day 16 test tell Capture from Select. */
enum class MediaSource { CAMERA, LIBRARY }

/**
 * Day 10 · `getMediaInfo()` returns this. Fields the system doesn't provide → `null` (like DeviceStatus in Week 3).
 * `width/height` = size **after EXIF rotation** — the orientation the user sees, matching iOS `UIImage.size`.
 * Mirrors `ios/.../NativeCore/Media/MediaInfo.swift`.
 */
data class MediaInfo(
    val name: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
    val width: Int?,
    val height: Int?,
    val source: MediaSource,
)

/** 1536 → "1.5 KB". Pure function — unit tested, iOS uses the same formula (1 KB = 1024 B). */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${oneDecimal(bytes / 1024.0)} KB"
    else -> "${oneDecimal(bytes / (1024.0 * 1024))} MB"
}

/** 1.46 → "1.5". Replaces `"%.1f".format(Locale.US, …)`, which is JVM-only; always a dot, like Locale.US. */
private fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

/**
 * Downscale factor when decoding (a power of 2) so the long side ≤ `maxSide`. A 12 MP camera photo decoded in full = ~48 MB RAM
 * → OutOfMemory on weak devices; a 1024 px preview is enough. This is "display", not "image processing" (out of scope).
 */
fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
    return sample
}
