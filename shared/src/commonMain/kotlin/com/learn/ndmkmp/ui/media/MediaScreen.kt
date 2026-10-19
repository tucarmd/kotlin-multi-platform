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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn.ndmkmp.presentation.media.MediaStrings
import com.learn.ndmkmp.presentation.media.MediaViewModel
import com.learn.ndmkmp.ui.common.AppCard
import com.learn.ndmkmp.ui.common.AppHintText
import com.learn.ndmkmp.ui.common.AppPrimaryButton
import com.learn.ndmkmp.ui.common.AppSecondaryButton
import com.learn.ndmkmp.ui.theme.AppColor
import com.learn.ndmkmp.ui.theme.AppSpacing
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Day 10 — Media tab: Capture / Pick → show the image + info. Draws [MediaViewModel.state].
 *
 * ```
 *  MediaScreen
 *   ├─ AppCard "Camera"         CAMERA permission · [Request] / [Capture photo] / [Open settings]
 *   ├─ AppCard "Media library"  [Pick from library]
 *   └─ AppCard "Selected image" image + info table (source · name · size · mime · dimensions) · [Clear]
 * ```
 */
@Composable
fun MediaScreen(vm: MediaViewModel) {
    val state by vm.state.collectAsState()
    // The ViewModel hands over encoded bytes; decoding them into a bitmap is the View's job (SwiftUI uses UIImage(data:)).
    val preview: ImageBitmap? = remember(state.image) {
        state.image?.preview?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
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
            if (state.cameraTextIsError) Text(state.cameraText, color = AppColor.danger, fontSize = 13.sp)
            else AppHintText(state.cameraText)
            AppPrimaryButton(text = state.captureButtonText, onClick = vm::onCaptureClick, enabled = state.captureEnabled)
            if (state.showOpenSettings) AppSecondaryButton(MediaStrings.OPEN_SETTINGS, onClick = vm::openSettings)
        }

        AppCard(title = MediaStrings.LIBRARY_TITLE) {
            AppHintText(MediaStrings.PICK_HINT)
            AppPrimaryButton(MediaStrings.PICK, onClick = vm::onPickClick, enabled = !state.loading)
        }

        AppCard(title = MediaStrings.RESULT_TITLE) {
            if (state.message.isNotEmpty()) Text(state.message, color = AppColor.textSecondary, fontSize = 13.sp)
            when {
                state.loading -> AppHintText(MediaStrings.LOADING)
                state.image == null -> AppHintText(MediaStrings.NOTHING_YET)
            }
            preview?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.infoRows.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(k, fontSize = 13.sp, color = AppColor.textSecondary, modifier = Modifier.weight(1f))
                        Text(v, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = AppColor.textPrimary)
                    }
                }
            }
            if (state.image != null) AppSecondaryButton(MediaStrings.CLEAR, onClick = vm::clearImage)
        }
    }
}
