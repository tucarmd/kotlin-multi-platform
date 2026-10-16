package com.learn.ndmkmp.core.media

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
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.learn.ndmkmp.core.permission.AndroidPermissions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * P5 — the real [MediaPicker] on Android. Day 10 — Camera & Media Manager, ported from the old app's `MediaManager`
 * (decode + EXIF + info) and the launcher code that used to live in its `MediaScreen`.
 *
 * ```
 *  Capture: capture() ─ newCaptureUri() ─► content://…fileprovider/images/capture_…jpg
 *              └ TakePicture(uri) ─► system Camera app WRITES into uri ─► true/false (false = Cancel)
 *  Select : pick() ─ PickVisualMedia(ImageOnly) ─► system Photo Picker ─► Uri? (null = Cancel)
 *  Then   : getMediaInfo(uri) + loadPreview(uri)   ← runs on Dispatchers.IO
 * ```
 *
 * ## Launcher → suspend, with a `CompletableDeferred`
 * An ActivityResult launcher answers in a CALLBACK, but [MediaPicker] is `suspend` so the common screen reads top to
 * bottom. The bridge: each call creates a `CompletableDeferred`, launches, and `await()`s; the launcher callback
 * `complete()`s it. One pending call per launcher is enough — the screen disables the buttons while loading.
 *
 * ```
 *  pick() ─► pending = CompletableDeferred() ─► launcher.launch() ─► await() …
 *                                               … Photo Picker …  ─► callback(uri) ─► pending.complete(uri) ─► resumes
 * ```
 *
 * Why the launchers are registered in the constructor: `registerForActivityResult` must run before the Activity is
 * STARTED (same rule as the permission launchers) — this class is built in `MainActivity.onCreate`.
 *
 * Why the system Intent (TakePicture) and not CameraX: the spec only needs "open native camera + capture", no
 * custom preview. CameraX = 3 libraries + lifecycle binding — overkill for this goal.
 *
 * ⚠️ Known limit vs the old screen: it kept the pending uri in `rememberSaveable`, so a photo survived Android killing
 * our process while the Camera app was open (low-RAM devices). Here the awaiting coroutine dies with the process →
 * the photo is lost and the user taps Capture again. Fine for a learning app; a real app would save the uri in
 * `SavedStateHandle`.
 */
class AndroidMediaPicker(private val activity: ComponentActivity) : MediaPicker {

    private val context: Context get() = activity.applicationContext

    /** Emulators/tablets may have no camera — the manifest declares `required=false` so it still installs. */
    override val hasCamera: Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private var pendingCapture: CompletableDeferred<Boolean>? = null
    private var pendingPick: CompletableDeferred<Uri?>? = null

    // ── CAMERA permission ──
    private val requestCameraLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingPermission?.complete(it)
    }

    // ── Capture: true = the Camera app wrote the photo into uri · false = user Cancel ──
    private val takePicture = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCapture?.complete(saved)
    }

    // ── Pick: system Photo Picker (API 33+; older devices fall back to Google Play / ACTION_OPEN_DOCUMENT) ──
    private val pickMedia = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        pendingPick?.complete(uri)
    }

    /** The Manifest DOES declare `CAMERA` → `TakePicture` without the permission is a SecurityException (crash). Check first. */
    override fun isCameraGranted(): Boolean = AndroidPermissions.isGranted(context, CAMERA_PERMISSION)

    override suspend fun requestCamera(): Boolean {
        // markAsked before launch — so the Permissions tab still tells DENIED / PERMANENTLY_DENIED apart correctly
        AndroidPermissions.markAsked(context, listOf(CAMERA_PERMISSION))
        val result = CompletableDeferred<Boolean>().also { pendingPermission = it }
        requestCameraLauncher.launch(CAMERA_PERMISSION)
        return result.await()
    }

    override suspend fun capture(): PickedImage? {
        if (!isCameraGranted()) return null   // never launch TakePicture without CAMERA → SecurityException
        val uri = newCaptureUri()
        val result = CompletableDeferred<Boolean>().also { pendingCapture = it }
        takePicture.launch(uri)
        if (!result.await()) return null
        return load(uri, MediaSource.CAMERA)
    }

    override suspend fun pick(): PickedImage? {
        val result = CompletableDeferred<Uri?>().also { pendingPick = it }
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        val uri = result.await() ?: return null
        return load(uri, MediaSource.LIBRARY)
    }

    override fun openSettings() = AndroidPermissions.openAppSettings(activity)

    /** Read info + decode on IO. Decoding a 12 MP image on main = jank / ANR. */
    private suspend fun load(uri: Uri, source: MediaSource): PickedImage = withContext(Dispatchers.IO) {
        val info = getMediaInfo(uri, source)
            ?: MediaInfo(name = null, sizeBytes = null, mimeType = null, width = null, height = null, source = source)
        PickedImage(preview = loadPreview(uri)?.asImageBitmap(), info = info)
    }

    /**
     * Create an empty slot for the Camera app to write the photo into. Must be `content://` via FileProvider — handing `file://`
     * to another app since API 24 is a `FileUriExposedException`. The file lives in `cacheDir` → the system cleans it when space runs low.
     */
    private fun newCaptureUri(): Uri {
        val dir = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Read info from the `ContentResolver` — the ONLY way with `content://` (there's no real file path).
     * IO → call from `Dispatchers.IO`. Read error → `null`, the UI reports it, no crash.
     */
    private fun getMediaInfo(uri: Uri, source: MediaSource): MediaInfo? = try {
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
        val rotated = rotationOf(uri) % 180 != 0
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
     * The downscale factor comes from common [sampleSizeFor] (unit tested on both platforms).
     */
    private fun loadPreview(uri: Uri, maxSide: Int = 1024): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) {
            null
        } else {
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide) }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val degrees = rotationOf(uri)
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
    private fun rotationOf(uri: Uri): Int = try {
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

    private companion object {
        const val TAG = "MediaManager"
        const val CAPTURE_DIR = "images"   // must match res/xml/file_paths.xml
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
