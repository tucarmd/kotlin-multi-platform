package com.learn.ndmkmp.platform

import com.learn.ndmkmp.core.media.MediaInfo
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.media.MediaSource
import com.learn.ndmkmp.core.media.PickedImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.posix.memcpy
import kotlin.coroutines.resume

/** What Swift hands back after capture / pick: a downscaled JPEG preview + the ORIGINAL image's info. */
class NativeImage(
    val preview: NSData?,
    val sizeBytes: Long,
    val mimeType: String?,
    val width: Int,
    val height: Int,
)

/**
 * R5b — the Swift half of the Media tab (`iosApp/.../Platform/MediaNative.swift`): presents UIKit pickers from the
 * top view controller. Callbacks on the main thread; `null` image = user cancelled.
 */
interface IosMediaNative {
    /** `UIImagePickerController.isSourceTypeAvailable(.camera)` — false on the Simulator. */
    fun hasCamera(): Boolean
    fun cameraGranted(): Boolean
    fun requestCamera(done: (Boolean) -> Unit)
    fun capture(done: (NativeImage?) -> Unit)
    fun pick(done: (NativeImage?) -> Unit)
    fun openSettings()
}

/**
 * R5b — [MediaPicker] for iOS: turns the Swift callbacks into `suspend` calls (Kotlin's `suspendCancellableCoroutine`
 * = Swift's `withCheckedContinuation`), so `MediaViewModel` reads top to bottom exactly like on Android.
 * iOS gives no file name for a picked photo (PHPicker runs out of process) → `name = null`.
 */
class IosMediaPicker(private val native: IosMediaNative) : MediaPicker {

    override val hasCamera: Boolean get() = native.hasCamera()

    override fun isCameraGranted(): Boolean = native.cameraGranted()

    override suspend fun requestCamera(): Boolean = suspendCancellableCoroutine { cont ->
        native.requestCamera { cont.resume(it) }
    }

    override suspend fun capture(): PickedImage? = suspendCancellableCoroutine { cont ->
        native.capture { cont.resume(it?.toPicked(MediaSource.CAMERA)) }
    }

    override suspend fun pick(): PickedImage? = suspendCancellableCoroutine { cont ->
        native.pick { cont.resume(it?.toPicked(MediaSource.LIBRARY)) }
    }

    override fun openSettings() = native.openSettings()

    private fun NativeImage.toPicked(source: MediaSource) = PickedImage(
        preview = preview?.toByteArray(),
        info = MediaInfo(name = null, sizeBytes = sizeBytes, mimeType = mimeType, width = width, height = height, source = source),
    )
}

/** NSData → ByteArray: pin the Kotlin array and copy the bytes in. */
@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
