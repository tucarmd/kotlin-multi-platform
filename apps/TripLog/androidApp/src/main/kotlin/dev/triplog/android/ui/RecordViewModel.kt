package dev.triplog.android.ui

import androidx.lifecycle.ViewModel
import dev.triplog.core.TripRecorder

/**
 * [App 4 · B5] Thin Android wrapper around the shared TripRecorder.
 *
 * TRAP: the trip state lives in TripRecorder (a Koin single), NOT in this ViewModel. A ViewModel dies when the user
 * leaves the screen with Back; rotation is fine either way, but a trip must outlive both.
 * Start is not here: it goes through TrackingService.start(context) so Android keeps the process alive.
 * Its iOS twin is TrackingModel.swift.
 */
class RecordViewModel(private val recorder: TripRecorder) : ViewModel() {
    val state = recorder.state

    fun pause() = recorder.pause()
    fun resume() = recorder.resume()
    fun stop() = recorder.stop()
}
