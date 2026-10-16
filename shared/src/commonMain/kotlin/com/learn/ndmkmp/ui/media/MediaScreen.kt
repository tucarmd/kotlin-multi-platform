package com.learn.ndmkmp.ui.media

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.core.media.MediaInfo
import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.media.PickedImage
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing
import kotlinx.coroutines.launch

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
 * KMP: the old Android screen held the ActivityResult launchers itself. Here the platform part is behind [MediaPicker]
 * (suspend until the user is done), so this file is the same on Android and iOS.
 */
@Composable
fun MediaScreen(media: MediaPicker) {
    val scope = rememberCoroutineScope()

    var cameraGranted by remember { mutableStateOf(media.isCameraGranted()) }
    var cameraDenied by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf<PickedImage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    /** Run one picker call: null = cancelled → keep the old image and say so. */
    fun show(cancelledText: String, call: suspend () -> PickedImage?) {
        scope.launch {
            loading = true
            val picked = call()
            loading = false
            if (picked == null) {
                message = cancelledText
            } else {
                shown = picked
                message = if (picked.preview == null) MediaStrings.LOAD_FAILED else ""
            }
        }
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
                !media.hasCamera -> AppHintText(MediaStrings.NO_CAMERA)
                cameraGranted -> AppHintText(MediaStrings.CAMERA_GRANTED)
                cameraDenied -> Text(MediaStrings.CAMERA_DENIED, color = AppColor.danger, fontSize = 13.sp)
                else -> AppHintText(MediaStrings.CAMERA_NOT_GRANTED)
            }
            AppPrimaryButton(
                text = if (cameraGranted) MediaStrings.CAPTURE else MediaStrings.REQUEST_CAMERA,
                onClick = {
                    cameraGranted = media.isCameraGranted()
                    if (cameraGranted) {
                        show(MediaStrings.CAPTURE_CANCELLED) { media.capture() }
                    } else {
                        // granted → open the camera right away (tapping Capture means they want a photo)
                        show(MediaStrings.CAPTURE_CANCELLED) {
                            val granted = media.requestCamera()
                            cameraGranted = granted
                            cameraDenied = !granted
                            if (granted) media.capture() else null
                        }
                    }
                },
                enabled = media.hasCamera && !loading,
            )
            if (cameraDenied) {
                AppSecondaryButton(MediaStrings.OPEN_SETTINGS, onClick = { media.openSettings() })
            }
        }

        AppCard(title = MediaStrings.LIBRARY_TITLE) {
            AppHintText(MediaStrings.PICK_HINT)
            AppPrimaryButton(MediaStrings.PICK, onClick = { show(MediaStrings.PICK_CANCELLED) { media.pick() } }, enabled = !loading)
        }

        AppCard(title = MediaStrings.RESULT_TITLE) {
            if (message.isNotEmpty()) Text(message, color = AppColor.textSecondary, fontSize = 13.sp)
            val current = shown
            when {
                loading -> AppHintText(MediaStrings.LOADING)
                current == null -> AppHintText(MediaStrings.NOTHING_YET)
            }
            current?.preview?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            current?.let { InfoTable(it.info) }
            if (current != null) {
                AppSecondaryButton(MediaStrings.CLEAR, onClick = { shown = null; message = "" })
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
