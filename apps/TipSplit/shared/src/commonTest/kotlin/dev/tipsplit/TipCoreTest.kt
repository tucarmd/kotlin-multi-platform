package dev.tipsplit

import dev.tipsplit.core.TipCore
import dev.tipsplit.model.SplitResult
import dev.tipsplit.model.SplitState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * [App 1 · B7] TipCore: each action updates the input and the result together.
 * No coroutines needed: StateFlow.value is read right after each call.
 */
class TipCoreTest {

    @Test
    fun `starts waiting for a bill`() {
        assertEquals(SplitResult.Waiting, TipCore().state.value.result)
    }

    @Test
    fun `typing a bill produces a result straight away`() {
        val core = TipCore()
        core.onBillChanged("100.000")
        core.addPerson() // 2 → 3 people
        val result = assertIs<SplitResult.Ready>(core.state.value.result)
        assertEquals(37_000, result.split.perPerson)
    }

    @Test
    fun `people never go below 1 or above the limit`() {
        val core = TipCore()
        repeat(5) { core.removePerson() }
        assertEquals(1, core.state.value.input.people)
        assertFalse(core.state.value.canRemovePerson)

        repeat(100) { core.addPerson() }
        assertEquals(SplitState.MAX_PEOPLE, core.state.value.input.people)
        assertFalse(core.state.value.canAddPerson)
    }

    @Test
    fun `turning rounding off shows the exact share`() {
        val core = TipCore()
        core.onBillChanged("100000")
        core.addPerson()
        core.onRoundUpToggled()
        assertEquals(36_667, assertIs<SplitResult.Ready>(core.state.value.result).split.perPerson)
    }

    @Test
    fun `reset goes back to the defaults`() {
        val core = TipCore()
        core.onBillChanged("50000")
        core.onTipSelected(15)
        core.reset()
        assertEquals(SplitState(), core.state.value)
    }
}
