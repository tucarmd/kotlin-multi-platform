package dev.tipsplit.android.ui

import androidx.lifecycle.ViewModel
import dev.tipsplit.core.TipCore

/**
 * [App 1 · B5] Thin Android wrapper around the shared TipCore.
 *
 * Why it exists at all: Compose screens get their state holder through `koinViewModel()`, which survives rotation.
 * It holds no logic: every function forwards to TipCore, which is tested in shared/commonTest.
 * Its iOS twin is SplitModel.swift.
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
