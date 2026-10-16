package dev.tipsplit.android.ui

import androidx.lifecycle.ViewModel
import dev.tipsplit.core.TipCore

/**
 * [App 1 · B5] Thin Android wrapper around the shared TipCore.
 *
 * Why it exists at all: Compose screens get their state holder through `koinViewModel()`, which survives rotation.
 * It holds no logic: every function forwards to TipCore, which is tested in shared/commonTest.
 * Its iOS twin is SplitModel.swift.
 *
 * [Bài 8 · ViewModel] Android keeps a ViewModel alive across rotation and drops it when the screen closes for good.
 * TipCore is a Koin single, so the state survives even longer: for the whole app process.
 * Koin builds this class (viewModelOf in TipSplitApp.kt) and passes TipCore to the constructor.
 */
class SplitViewModel(private val core: TipCore) : ViewModel() {
    val state = core.state
    val tipOptions = core.tipOptions

    fun onBillChanged(text: String) = core.onBillChanged(text)
    fun onTipSelected(percent: Int) = core.onTipSelected(percent)
    fun addPerson() = core.addPerson()
    fun removePerson() = core.removePerson()
    fun onRoundUpToggled() = core.onRoundUpToggled()
    fun reset() = core.reset()
}
