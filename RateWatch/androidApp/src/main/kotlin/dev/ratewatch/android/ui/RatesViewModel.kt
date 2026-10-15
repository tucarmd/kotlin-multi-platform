package dev.ratewatch.android.ui

import androidx.lifecycle.ViewModel
import dev.ratewatch.core.RatesCore

/**
 * [App 3 · B5] Thin Android wrapper around the shared RatesCore, used by both tabs.
 *
 * Why it exists at all: Compose screens get their state holder through `koinViewModel()`, which survives rotation.
 * It holds no logic: every function forwards to RatesCore, which is tested in shared/commonTest.
 * Its iOS twin is RatesModel.swift.
 */
class RatesViewModel(private val core: RatesCore) : ViewModel() {
    val state = core.state
    val convert = core.convert

    fun start() = core.start()
    fun refresh() = core.refresh()
    fun selectBase(code: String) = core.selectBase(code)
    fun toggleFavorite(code: String) = core.toggleFavorite(code)
    fun onAmountChanged(text: String) = core.onAmountChanged(text)
    fun onFromSelected(code: String) = core.onFromSelected(code)
    fun onToSelected(code: String) = core.onToSelected(code)
    fun swapCurrencies() = core.swapCurrencies()
}
