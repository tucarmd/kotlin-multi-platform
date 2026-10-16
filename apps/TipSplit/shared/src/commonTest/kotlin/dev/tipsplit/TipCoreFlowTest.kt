package dev.tipsplit

import app.cash.turbine.test
import dev.tipsplit.core.TipCore
import dev.tipsplit.model.SplitResult
import dev.tipsplit.model.SplitState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * [Phụ lục P1 · Coroutine & Flow] TipCore.state seen as a FLOW, the way the UIs see it
 * (Android collectAsStateWithLifecycle, Swift `for await`).
 *
 * TipCoreTest reads `state.value` after each call. That checks the end result but not WHAT THE UI RECEIVES:
 * how many emissions, in which order, and whether a no-op action redraws the screen. Turbine checks that.
 */
class TipCoreFlowTest {

    @Test
    fun `each action emits exactly one new state in order`() = runTest {
        val core = TipCore()
        core.state.test {
            // A StateFlow always replays its current value first.
            assertEquals(SplitResult.Waiting, awaitItem().result)

            core.onBillChanged("100.000")
            assertEquals(55_000, assertIs<SplitResult.Ready>(awaitItem().result).split.perPerson) // 110.000 / 2 people

            core.addPerson()
            val three = awaitItem()
            assertEquals(3, three.input.people)
            assertEquals(37_000, assertIs<SplitResult.Ready>(three.result).split.perPerson)

            // Nothing else may arrive: an extra emission would mean an extra redraw.
            expectNoEvents()
        }
    }

    @Test
    fun `an action that changes nothing does not emit`() = runTest {
        val core = TipCore()
        core.state.test {
            awaitItem() // initial, people = 2
            core.removePerson()
            assertEquals(1, awaitItem().input.people)

            // Already at the minimum: TipCore computes the same SplitState, and StateFlow drops values
            // EQUAL to the current one. Data classes give us that equality for free.
            core.removePerson()
            core.onTipSelected(10) // 10 % is already selected
            expectNoEvents()
        }
    }

    @Test
    fun `a slow collector only sees the latest state because StateFlow conflates`() = runTest {
        val core = TipCore()
        val seen = mutableListOf<SplitState>()
        // runTest uses StandardTestDispatcher: this collector only runs when the test yields.
        val job = launch { core.state.toList(seen) }
        advanceUntilIdle()

        // Three updates with no suspension in between. StateFlow keeps ONE value, so the collector
        // wakes up once and finds only the last one. This is why the UI never shows "intermediate" states,
        // and why a test that expects every step must use Turbine (unconfined) instead of a plain collect.
        core.onBillChanged("1")
        core.onBillChanged("10")
        core.onBillChanged("100.000")
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf("", "100.000"), seen.map { it.input.billText })
    }
}
