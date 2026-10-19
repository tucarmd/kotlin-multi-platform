package com.learn.ndmkmp.fake

import com.learn.ndmkmp.core.media.MediaInfo
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.media.MediaSource
import com.learn.ndmkmp.core.media.PickedImage
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

    override suspend fun capture() = fakeImage(MediaSource.CAMERA, "IMG_demo.jpg", sky = 0xFF1E5AA8.toInt())

    override suspend fun pick() = fakeImage(MediaSource.LIBRARY, "library_demo.png", sky = 0xFF2E9E5B.toInt())

    override fun openSettings() {
        granted = true
    }

    private suspend fun fakeImage(source: MediaSource, name: String, sky: Int): PickedImage? {
        delay(600)
        if (++calls % 3 == 0) return null
        return PickedImage(
            preview = drawPicture(sky),
            info = MediaInfo(name, sizeBytes = 245_760, mimeType = if (name.endsWith("png")) "image/png" else "image/jpeg",
                width = W, height = H, source = source),
        )
    }

    /**
     * A 320×240 "photo" as a BMP file: coloured sky, white sun, dark ground — enough to see aspect ratio.
     * BMP because it is the simplest image format to write by hand (header + raw pixels), and both
     * Android `BitmapFactory` and iOS `UIImage(data:)` decode it.
     */
    private fun drawPicture(sky: Int): ByteArray {
        val rowBytes = W * 3                       // 24-bit, W*3 is already a multiple of 4 → no row padding
        val pixelBytes = rowBytes * H
        val out = ByteArray(54 + pixelBytes)
        fun put32(at: Int, v: Int) { for (i in 0..3) out[at + i] = (v shr (8 * i)).toByte() }
        fun put16(at: Int, v: Int) { for (i in 0..1) out[at + i] = (v shr (8 * i)).toByte() }
        out[0] = 'B'.code.toByte(); out[1] = 'M'.code.toByte()
        put32(2, out.size); put32(10, 54)          // file size · pixel data offset
        put32(14, 40); put32(18, W); put32(22, H)  // BITMAPINFOHEADER · width · height (positive = bottom-up rows)
        put16(26, 1); put16(28, 24); put32(34, pixelBytes)
        val sunX = W * 3 / 4; val sunY = H * 3 / 10; val sunR = 30
        for (y in 0 until H) {                     // y = 0 is the TOP row of the picture
            for (x in 0 until W) {
                val dx = x - sunX; val dy = y - sunY
                val argb = when {
                    y > H * 7 / 10 -> 0xFF1C2B39.toInt()
                    dx * dx + dy * dy <= sunR * sunR -> 0xFFFFFFFF.toInt()
                    else -> sky
                }
                val at = 54 + (H - 1 - y) * rowBytes + x * 3
                out[at] = argb.toByte(); out[at + 1] = (argb shr 8).toByte(); out[at + 2] = (argb shr 16).toByte()   // B G R
            }
        }
        return out
    }

    private companion object {
        const val W = 320
        const val H = 240
    }
}
