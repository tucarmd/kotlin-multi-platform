package com.learn.ndmkmp.presentation.media

import com.learn.ndmkmp.core.media.MediaInfo
import com.learn.ndmkmp.core.media.formatBytes

/** Media tab strings (Day 10). Mirrors `UI/Media/MediaStrings.swift`. */
object MediaStrings {
    const val HINT = "Day 10 — capture a photo with the system camera or pick one from the library, then show it with its info."

    const val CAMERA_TITLE = "Camera"
    const val CAPTURE = "Capture photo"
    const val REQUEST_CAMERA = "Request camera permission"
    const val OPEN_SETTINGS = "Open app settings"
    const val CAMERA_GRANTED = "Camera permission: GRANTED"
    const val CAMERA_NOT_GRANTED = "Camera permission: not granted — Capture asks first."
    const val CAMERA_DENIED = "Camera permission denied. Capture stays disabled until you allow it (Settings if it no longer asks)."
    const val NO_CAMERA = "This device has no camera — use Pick from library."

    const val LIBRARY_TITLE = "Media library"
    const val PICK = "Pick from library"
    const val PICK_HINT = "System Photo Picker — no storage permission needed, the app only sees the photo you choose."

    const val RESULT_TITLE = "Selected image"
    const val NOTHING_YET = "No image yet."
    const val LOADING = "Loading image…"
    const val CAPTURE_CANCELLED = "Capture cancelled — nothing changed."
    const val PICK_CANCELLED = "Pick cancelled — nothing changed."
    const val LOAD_FAILED = "Could not read the image."
    const val CLEAR = "Clear"

    fun infoRows(info: MediaInfo): List<Pair<String, String>> = listOf(
        "source" to info.source.name.lowercase(),
        "name" to (info.name ?: "null"),
        "size" to (info.sizeBytes?.let { "${formatBytes(it)} ($it B)" } ?: "null"),
        "mime" to (info.mimeType ?: "null"),
        "dimensions" to if (info.width != null && info.height != null) "${info.width} × ${info.height} px" else "null",
    )
}
