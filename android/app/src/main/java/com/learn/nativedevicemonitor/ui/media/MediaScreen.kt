package com.learn.nativedevicemonitor.ui.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.nativedevicemonitor.nativecore.media.MediaInfo
import com.learn.nativedevicemonitor.nativecore.media.MediaManager
import com.learn.nativedevicemonitor.nativecore.media.MediaSource
import com.learn.nativedevicemonitor.nativecore.permission.PermissionService
import com.learn.nativedevicemonitor.ui.common.AppCard
import com.learn.nativedevicemonitor.ui.common.AppHintText
import com.learn.nativedevicemonitor.ui.common.AppPrimaryButton
import com.learn.nativedevicemonitor.ui.common.AppSecondaryButton
import com.learn.nativedevicemonitor.ui.theme.AppColor
import com.learn.nativedevicemonitor.ui.theme.AppSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Day 10 — Media tab: Capture / Pick → show the image + info.
 *
 * ```
 *  MediaScreen
 *   ├─ AppCard "Camera"         CAMERA permission · [Request] / [Capture photo] / [Open settings]
 *   ├─ AppCard "Media library"  [Pick from library]
 *   └─ AppCard "Selected image" image + info table (source · name · size · mime · dimensions) · [Clear]
 * ```
 *
 * Launchers use `rememberLauncherForActivityResult` (the Compose version of `registerForActivityResult`) — it registers
 * at the right time so it can be called in a Composable; the Permissions tab keeps launchers in the Activity because the Activity needs that state in `onResume`.
 *
 * `rememberSaveable` for the 2 uris: opening the Camera app → Android may KILL our app process (low-RAM devices) → on return
 * we must remember the pending uri, otherwise the photo just taken is lost.
 */
@Composable
fun MediaScreen() {
    val context = LocalContext.current.applicationContext
    val hasCamera = remember { MediaManager.hasCamera(context) }

    var cameraGranted by remember { mutableStateOf(MediaManager.isCameraPermissionGranted(context)) }
    var cameraDenied by remember { mutableStateOf(false) }
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var shownUri by rememberSaveable { mutableStateOf<String?>(null) }
    var shownSource by rememberSaveable { mutableStateOf(MediaSource.LIBRARY) }

    var info by remember { mutableStateOf<MediaInfo?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    // ── Capture: true = the Camera app wrote the photo into uri · false = user Cancel ──
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (saved && uri != null) {
            shownSource = MediaSource.CAMERA
            shownUri = uri
            message = ""
        } else {
            message = MediaStrings.CAPTURE_CANCELLED
        }
    }

    fun launchCamera() {
        val uri = MediaManager.newCaptureUri(context)
        pendingCaptureUri = uri.toString()
        takePicture.launch(uri)
    }

    // ── CAMERA permission: granted → open the camera right away (tapping Capture means they want a photo) ──
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraDenied = !granted
        if (granted) launchCamera()
    }

    // ── Pick: system Photo Picker (API 33+; older devices fall back to Google Play / ACTION_OPEN_DOCUMENT) ──
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            shownSource = MediaSource.LIBRARY
            shownUri = uri.toString()
            message = ""
        } else {
            message = MediaStrings.PICK_CANCELLED
        }
    }

    // Image changed → read info + decode on IO. Decoding a 12 MP image on main = jank / ANR.
    LaunchedEffect(shownUri) {
        val uri = shownUri?.let(Uri::parse)
        if (uri == null) {
            info = null; bitmap = null
            return@LaunchedEffect
        }
        loading = true
        val (newInfo, newBitmap) = withContext(Dispatchers.IO) {
            MediaManager.getMediaInfo(context, uri, shownSource) to MediaManager.loadPreview(context, uri)
        }
        info = newInfo
        bitmap = newBitmap
        loading = false
        if (newBitmap == null) message = MediaStrings.LOAD_FAILED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.contentBackground)
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppHintText(MediaStrings.HINT, bullet = false)

        AppCard(title = MediaStrings.CAMERA_TITLE) {
            when {
                !hasCamera -> AppHintText(MediaStrings.NO_CAMERA)
                cameraGranted -> AppHintText(MediaStrings.CAMERA_GRANTED)
                cameraDenied -> Text(MediaStrings.CAMERA_DENIED, color = AppColor.danger, fontSize = 13.sp)
                else -> AppHintText(MediaStrings.CAMERA_NOT_GRANTED)
            }
            AppPrimaryButton(
                text = if (cameraGranted) MediaStrings.CAPTURE else MediaStrings.REQUEST_CAMERA,
                onClick = {
                    // Re-read on every tap: the user may have just toggled the permission in Settings and come back (no event)
                    cameraGranted = MediaManager.isCameraPermissionGranted(context)
                    if (cameraGranted) {
                        launchCamera()
                    } else {
                        // markAsked before launch — so the Permissions tab still tells DENIED / PERMANENTLY_DENIED apart correctly
                        PermissionService.markAsked(context, listOf(MediaManager.CAMERA_PERMISSION))
                        requestCamera.launch(MediaManager.CAMERA_PERMISSION)
                    }
                },
                enabled = hasCamera,
            )
            if (cameraDenied) {
                AppSecondaryButton(MediaStrings.OPEN_SETTINGS, onClick = { PermissionService.openAppSettings(context) })
            }
        }

        AppCard(title = MediaStrings.LIBRARY_TITLE) {
            AppHintText(MediaStrings.PICK_HINT)
            AppPrimaryButton(MediaStrings.PICK, onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
        }

        AppCard(title = MediaStrings.RESULT_TITLE) {
            if (message.isNotEmpty()) Text(message, color = AppColor.textSecondary, fontSize = 13.sp)
            val shown = bitmap
            when {
                loading -> AppHintText(MediaStrings.LOADING)
                shown == null && info == null -> AppHintText(MediaStrings.NOTHING_YET)
            }
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            info?.let { InfoTable(it) }
            if (shownUri != null) {
                AppSecondaryButton(MediaStrings.CLEAR, onClick = { shownUri = null; message = "" })
            }
        }
    }
}

@Composable
private fun InfoTable(info: MediaInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MediaStrings.infoRows(info).forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(k, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.weight(1f))
                Text(v, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
            }
        }
    }
}
