package dev.devicepulse.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.devicepulse.core.PulseCore

/** Thin Android wrapper: the shared PulseMonitor does the work, viewModelScope keeps it alive across rotations. */
class PulseViewModel(core: PulseCore) : ViewModel() {
    val monitor = core.startMonitor(viewModelScope)
}
