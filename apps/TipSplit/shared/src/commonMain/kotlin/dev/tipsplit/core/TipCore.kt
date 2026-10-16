package dev.tipsplit.core

import dev.tipsplit.model.SplitInput
import dev.tipsplit.model.SplitState
import dev.tipsplit.rules.TIP_OPTIONS
import dev.tipsplit.rules.calculate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [App 1 · B2] TipCore: holds the screen state and receives every user action. Shared by both UIs.
 *
 * Why a core class instead of putting this in the Android ViewModel: iOS has no ViewModel. Keeping state here means
 * Android's SplitViewModel and iOS's SplitModel are both a few lines of forwarding, and the logic is tested once.
 *
 * Pattern used by all 5 practice apps: every action changes the INPUT, then the whole state is recomputed from it
 * with a pure rule (`calculate`). The result can never get out of sync with the input.
 *
 * Called by: SplitViewModel (Android), SplitModel (iOS). Calls: rules/calculate.
 *
 * [Bài 8 · ViewModel] The lesson keeps state in a multiplatform androidx ViewModel inside composeApp, because there
 * both platforms draw with Compose. Tip Split has a SwiftUI side, so the state lives in this plain class instead,
 * and each UI wraps it in its own holder: SplitViewModel (Android ViewModel) and SplitModel (Swift @Observable).
 */
class TipCore {

    // Backing property: only TipCore writes, everyone else reads the read-only StateFlow below.
    private val _state = MutableStateFlow(SplitState())

    /** The current screen state. Android: collectAsStateWithLifecycle(). Swift: `for await` (SKIE). */
    val state: StateFlow<SplitState> = _state.asStateFlow()

    /** Tip buttons to show, so neither UI hard-codes 0/5/10/15. */
    val tipOptions: List<Int> = TIP_OPTIONS

    /** Stores the text as typed (not the parsed number) so "120." stays "120." while the user is still typing. */
    fun onBillChanged(text: String) = update { it.copy(billText = text) }

    fun onTipSelected(percent: Int) = update { it.copy(tipPercent = percent.coerceIn(0, 100)) }

    fun addPerson() = update { it.copy(people = (it.people + 1).coerceAtMost(SplitState.MAX_PEOPLE)) }

    fun removePerson() = update { it.copy(people = (it.people - 1).coerceAtLeast(1)) }

    fun onRoundUpToggled() = update { it.copy(roundUp = !it.roundUp) }

    /** Back to the default input: empty bill, 10 %, 2 people, rounding on. */
    fun reset() = update { SplitInput() }

    /** The only place that writes the state: apply the change to the input, then recompute the result. */
    private fun update(change: (SplitInput) -> SplitInput) {
        // MutableStateFlow.update { } is atomic: two quick taps cannot overwrite each other.
        _state.update { old ->
            val input = change(old.input)
            SplitState(input = input, result = calculate(input))
        }
    }
}
