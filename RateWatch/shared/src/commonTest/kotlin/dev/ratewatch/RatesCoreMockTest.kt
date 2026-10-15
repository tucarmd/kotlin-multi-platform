package dev.ratewatch

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verify.VerifyMode.Companion.order
import dev.mokkery.verifySuspend
import dev.ratewatch.core.RatesCore
import dev.ratewatch.data.RateApiException
import dev.ratewatch.data.RateRepository
import dev.ratewatch.model.RefreshError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [Phụ lục P1 · Fake & mock] The same RatesCore as RatesCoreTest, but with a MOCK repository (Mokkery) instead of
 * the hand-written FakeRateRepository. Compare the two files:
 *
 * - FakeRateRepository is a tiny working implementation (MutableStateFlow-backed). Tests check the RESULT: state.
 * - A mock has no behaviour until you script it (`every { } returns …`). Tests check the CALLS: which method,
 *   with which arguments, how many times, in which order.
 *
 * Rule of thumb used in this repo: fake by default (tests survive refactors), mock only when the call itself
 * is the behaviour under test ("download exactly once", "remember the base BEFORE downloading it").
 */
class RatesCoreMockTest {

    // Mokkery generates the mock class at compile time (compiler plugin), so this works on iOS too.
    // MockK would not: it relies on JVM reflection and only runs in androidUnitTest.
    private val repo = mock<RateRepository> {
        every { observeBase() } returns flowOf(null)
        every { observe(any()) } returns flowOf(null)
        every { observeFavorites() } returns flowOf(emptySet())
        everySuspend { setBase(any()) } returns Unit
    }

    private var coreScope: CoroutineScope? = null

    private fun TestScope.core(): RatesCore {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        coreScope = scope
        return RatesCore(repo, scope = scope)
    }

    @AfterTest
    fun stopCore() {
        coreScope?.cancel()
    }

    @Test
    fun `taps on refresh while downloading call the network only once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        everySuspend { repo.refresh(any(), any()) } calls { gate.await(); Result.success(Unit) }
        val core = core()

        repeat(3) { core.refresh() }
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        verifySuspend(exactly(1)) { repo.refresh("USD", any()) }
    }

    @Test
    fun `selecting a base saves it before downloading it`() = runTest {
        everySuspend { repo.refresh(any(), any()) } returns Result.success(Unit)
        val core = core()
        advanceUntilIdle()

        core.selectBase("EUR")
        advanceUntilIdle()

        // ORDER matters: if the download ran first and the app was killed, the next launch would reopen on USD.
        verifySuspend(order) {
            repo.setBase("EUR")
            repo.refresh("EUR", any())
        }
    }

    @Test
    fun `a scripted failure shows up as the screen error`() = runTest {
        everySuspend { repo.refresh(any(), any()) } returns Result.failure(RateApiException(RefreshError.Timeout))
        val core = core()

        core.refresh()
        advanceUntilIdle()

        assertEquals(RefreshError.Timeout, core.state.value.error)
    }
}
