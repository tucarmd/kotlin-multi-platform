package dev.obdlink.android.ui

import androidx.lifecycle.ViewModel
import dev.obdlink.core.ObdCore
import dev.obdlink.obd.model.Dongle

/**
 * [App 5 · B5] Thin Android wrapper around the shared ObdCore.
 *
 * Why it exists: Compose screens get their state holder through `koinViewModel()`, which survives rotation.
 * It holds no logic: every function forwards to ObdCore, which is tested in shared/commonTest.
 * Its iOS twin is ObdModel.swift.
 */
class ObdViewModel(private val core: ObdCore) : ViewModel() {
    val state = core.state

    fun scan() = core.scan()
    fun stopScan() = core.stopScan()
    fun connect(dongle: Dongle) = core.connect(dongle)
    fun disconnect() = core.disconnect()
    fun reset() = core.reset()
    fun startLive() = core.startLive()
    fun stopLive() = core.stopLive()
    fun readCodes() = core.readCodes()
    fun requestClearCodes() = core.requestClearCodes()
    fun confirmClearCodes() = core.confirmClearCodes()
    fun cancelClearCodes() = core.cancelClearCodes()
    fun sendConsole(text: String) = core.sendConsole(text)
    fun clearConsole() = core.clearConsole()

    // PLATFORM: no onCleared() disconnect on purpose. ObdCore is an app-wide singleton; the ViewModel dies on
    // "back" while the user may come straight back. Disconnecting is an explicit button.
}
