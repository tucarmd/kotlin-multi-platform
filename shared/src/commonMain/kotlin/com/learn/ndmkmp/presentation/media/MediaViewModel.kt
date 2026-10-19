package com.learn.ndmkmp.presentation.media

import com.learn.ndmkmp.core.media.MediaPicker
import com.learn.ndmkmp.core.media.PickedImage
import com.learn.ndmkmp.presentation.InfoRow
import com.learn.ndmkmp.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MediaUiState(
    val hasCamera: Boolean = true,
    val cameraGranted: Boolean = false,
    val cameraDenied: Boolean = false,
    val loading: Boolean = false,
    /** Cancelled / could not read — grey line on the result card. */
    val message: String = "",
    val image: PickedImage? = null,
) {
    /** Line under "Camera" + whether it is red. */
    val cameraText: String get() = when {
        !hasCamera -> MediaStrings.NO_CAMERA
        cameraGranted -> MediaStrings.CAMERA_GRANTED
        cameraDenied -> MediaStrings.CAMERA_DENIED
        else -> MediaStrings.CAMERA_NOT_GRANTED
    }
    val cameraTextIsError: Boolean get() = hasCamera && !cameraGranted && cameraDenied
    val captureButtonText: String get() = if (cameraGranted) MediaStrings.CAPTURE else MediaStrings.REQUEST_CAMERA
    val captureEnabled: Boolean get() = hasCamera && !loading
    val showOpenSettings: Boolean get() = cameraDenied
    val infoRows: List<InfoRow> get() = image?.let { MediaStrings.infoRows(it.info) }.orEmpty().map { (k, v) -> InfoRow(k, v) }
}

/** Day 10 — Media tab. Every [MediaPicker] call suspends until the user is done → this file reads top to bottom. */
class MediaViewModel(private val media: MediaPicker) : SharedViewModel() {

    private val _state = MutableStateFlow(MediaUiState(hasCamera = media.hasCamera, cameraGranted = media.isCameraGranted()))
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    /** Capture button: granted → camera; not yet → ask, and if granted open the camera right away. */
    fun onCaptureClick() {
        val granted = media.isCameraGranted()     // re-read: may have just changed in Settings
        _state.update { it.copy(cameraGranted = granted) }
        show(MediaStrings.CAPTURE_CANCELLED) {
            if (granted) {
                media.capture()
            } else {
                val ok = media.requestCamera()
                _state.update { it.copy(cameraGranted = ok, cameraDenied = !ok) }
                if (ok) media.capture() else null
            }
        }
    }

    fun onPickClick() = show(MediaStrings.PICK_CANCELLED) { media.pick() }

    fun clearImage() = _state.update { it.copy(image = null, message = "") }

    fun openSettings() = media.openSettings()

    /** Run one picker call: null = cancelled → keep the old image and say so. */
    private fun show(cancelledText: String, call: suspend () -> PickedImage?) {
        if (_state.value.loading) return
        scope.launch {
            _state.update { it.copy(loading = true) }
            val picked = call()
            _state.update {
                when {
                    picked == null -> it.copy(loading = false, message = cancelledText)
                    else -> it.copy(loading = false, image = picked, message = if (picked.preview == null) MediaStrings.LOAD_FAILED else "")
                }
            }
        }
    }
}
