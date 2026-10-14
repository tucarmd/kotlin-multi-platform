package dev.devicepulse.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import dev.devicepulse.android.DevicePulseApp
import dev.devicepulse.core.PulseCore

/** Thin Android wrapper: the shared PulseMonitor does the work, viewModelScope keeps it alive across rotations. */
class PulseViewModel(core: PulseCore) : ViewModel() {
    val monitor = core.startMonitor(viewModelScope)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PulseViewModel((this[APPLICATION_KEY] as DevicePulseApp).core) }
        }
    }
}
