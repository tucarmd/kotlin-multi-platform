package com.learn.nativedevicemonitor.nativecore.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.learn.nativedevicemonitor.nativecore.permission.PermissionService
import java.io.File
import java.io.IOException

/**
 * Day 10 — Camera & Media Manager. API per the Word spec: `requestCameraPermission()` (launcher in the UI, see below) ·
 * `capturePhoto()` = [newCaptureUri] + `TakePicture` · `pickMedia()` = `PickVisualMedia` · [getMediaInfo] · [loadPreview].
 *
 * ```
 *  Capture: UI ─ newCaptureUri() ─► content://…fileprovider/images/capture_…jpg
 *              └ TakePicture(uri) ─► system Camera app WRITES into uri ─► true/false (false = Cancel)
 *  Select : UI ─ PickVisualMedia(ImageOnly) ─► system Photo Picker ─► Uri? (null = Cancel)
 *  Then   : getMediaInfo(uri) + loadPreview(uri)   ← runs on Dispatchers.IO
 * ```
 *
 * Why the system Intent (TakePicture) and not CameraX: the spec only needs "open native camera + capture", no
 * custom preview. CameraX = 3 libraries + lifecycle binding — overkill for this goal.
 *
 * Why the launchers (`rememberLauncherForActivityResult`) live in the UI and not here: a launcher is bound to
 * a live Activity/Composable; a singleton object holding a launcher = leaked Activity. Only pure logic + IO remain here.
 */
object MediaManager {

    private const val TAG = "MediaManager"
    private const val CAPTURE_DIR = "images"   // must match res/xml/file_paths.xml

    /** The Manifest DOES declare `CAMERA` → `TakePicture` without the permission is a SecurityException (crash). Check first. */
    fun isCameraPermissionGranted(context: Context): Boolean =
        PermissionService.isGranted(context, Manifest.permission.CAMERA)

    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    /** Emulators/tablets may have no camera — the manifest declares `required=false` so it still installs. */
    fun hasCamera(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    /**
     * Create an empty slot for the Camera app to write the photo into. Must be `content://` via FileProvider — handing `file://`
     * to another app since API 24 is a `FileUriExposedException`. The file lives in `cacheDir` → the system cleans it when space runs low.
     */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Read info from the `ContentResolver` — the ONLY way with `content://` (there's no real file path).
     * IO → call from `Dispatchers.IO`. Read error → `null`, the UI reports it, no crash.
     */
    fun getMediaInfo(context: Context, uri: Uri, source: MediaSource): MediaInfo? = try {
        val resolver = context.contentResolver
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0)
                size = if (c.isNull(1)) null else c.getLong(1)
            }
        }
        // inJustDecodeBounds: only read the header for the size, don't decode the image → fast, no RAM cost
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val rotated = rotationOf(context, uri) % 180 != 0
        MediaInfo(
            name = name,
            sizeBytes = size,
            mimeType = resolver.getType(uri) ?: bounds.outMimeType,
            width = (if (rotated) bounds.outHeight else bounds.outWidth).takeIf { it > 0 },
            height = (if (rotated) bounds.outWidth else bounds.outHeight).takeIf { it > 0 },
            source = source,
        )
    } catch (e: IOException) {
        Log.w(TAG, "getMediaInfo failed: $uri", e)
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "getMediaInfo no access: $uri", e)   // uri lost its read grant (e.g. after the process died)
        null
    }

    /**
     * Decode a downscaled image for display + rotate per EXIF (Samsung stores portrait photos as landscape + a rotation flag).
     * `null` = unreadable / empty file (Cancel after the Camera app already created a 0-byte file).
     */
    fun loadPreview(context: Context, uri: Uri, maxSide: Int = 1024): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) {
            null
        } else {
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide) }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val degrees = rotationOf(context, uri)
            if (bitmap == null || degrees == 0) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        }
    } catch (e: IOException) {
        Log.w(TAG, "loadPreview failed: $uri", e)
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "loadPreview no access: $uri", e)
        null
    }

    /** `android.media.ExifInterface(InputStream)` exists since API 24 — minSdk 26 so no androidx.exifinterface library needed. */
    private fun rotationOf(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: IOException) {
        0   // PNG/GIF have no EXIF → treat as not rotated
    }
}
