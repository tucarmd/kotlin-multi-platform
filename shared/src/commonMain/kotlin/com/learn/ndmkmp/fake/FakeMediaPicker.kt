package com.learn.ndmkmp.fake

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import com.learn.ndmkmp.core.media.MediaInfo
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.media.MediaSource
import com.learn.ndmkmp.core.media.PickedImage
import com.learn.ndmkmp.ui.theme.AppColor
import kotlinx.coroutines.delay

/**
 * P2 stand-in for the camera / photo picker (P5). No system UI: each call waits a moment (like the user choosing) and
 * returns a picture drawn in code. Every 3rd call returns null = "user cancelled", so the cancel message shows too.
 */
class FakeMediaPicker : MediaPicker {

    override val hasCamera = true
    private var granted = false
    private var calls = 0

    override fun isCameraGranted() = granted

    override suspend fun requestCamera(): Boolean {
        delay(300)
        granted = true
        return true
    }

    override suspend fun capture() = fakeImage(MediaSource.CAMERA, "IMG_demo.jpg", AppColor.primary)

    override suspend fun pick() = fakeImage(MediaSource.LIBRARY, "library_demo.png", AppColor.success)

    override fun openSettings() {
        granted = true
    }

    private suspend fun fakeImage(source: MediaSource, name: String, color: Color): PickedImage? {
        delay(600)
        if (++calls % 3 == 0) return null
        return PickedImage(
            preview = drawPicture(color),
            info = MediaInfo(name, sizeBytes = 245_760, mimeType = if (name.endsWith("png")) "image/png" else "image/jpeg",
                width = W, height = H, source = source),
        )
    }

    /** A 640×480 "photo": coloured sky, white sun, dark ground — enough to see aspect ratio and ContentScale.Fit. */
    private fun drawPicture(sky: Color): ImageBitmap {
        val image = ImageBitmap(W, H)
        val canvas = Canvas(image)
        val paint = Paint()
        paint.color = sky
        canvas.drawRect(Rect(0f, 0f, W.toFloat(), H.toFloat()), paint)
        paint.color = Color.White
        canvas.drawCircle(Offset(W * 0.75f, H * 0.3f), 60f, paint)
        paint.color = AppColor.textPrimary
        canvas.drawRect(Rect(0f, H * 0.7f, W.toFloat(), H.toFloat()), paint)
        return image
    }

    private companion object {
        const val W = 640
        const val H = 480
    }
}
