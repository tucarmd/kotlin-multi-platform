package com.learn.ndmkmp.core.media

import androidx.compose.ui.graphics.ImageBitmap

/** An image the user captured or picked: a downscaled preview (null = could not decode) + its info. */
class PickedImage(val preview: ImageBitmap?, val info: MediaInfo)

/**
 * What the Media tab needs. Every call suspends until the user is done → the screen reads top to bottom, no callbacks.
 * P2: [com.learn.ndmkmp.fake.FakeMediaPicker] · P5: Android (TakePicture / PickVisualMedia launchers) and
 * iOS (UIImagePickerController / PHPicker presented from Kotlin).
 */
interface MediaPicker {
    /** Device has a camera at all (the iOS Simulator does not). */
    val hasCamera: Boolean

    /** Re-read on every tap: the user may have just changed it in Settings (no event fires). */
    fun isCameraGranted(): Boolean

    /** Shows the system dialog, true = granted. */
    suspend fun requestCamera(): Boolean

    /** null = user cancelled. */
    suspend fun capture(): PickedImage?

    /** null = user cancelled. */
    suspend fun pick(): PickedImage?

    fun openSettings()
}
