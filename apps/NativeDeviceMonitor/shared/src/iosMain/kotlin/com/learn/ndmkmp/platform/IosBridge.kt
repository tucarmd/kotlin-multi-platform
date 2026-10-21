package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.media.PickedImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

/**
 * Small helpers for Swift where the Kotlin type is awkward on the Swift side.
 * Swift: `IosBridgeKt.previewData(image:)` → `Data` → `UIImage(data:)`.
 */
@OptIn(ExperimentalForeignApi::class)
fun previewData(image: PickedImage): NSData? {
    val bytes = image.preview ?: return null
    if (bytes.isEmpty()) return null
    // Pin the Kotlin array so its address can't move while NSData copies it.
    return bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
}
