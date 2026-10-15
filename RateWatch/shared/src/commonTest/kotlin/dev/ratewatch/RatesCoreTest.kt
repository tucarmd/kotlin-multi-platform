package dev.ratewatch

import app.cash.turbine.test
import dev.ratewatch.core.RatesCore
import dev.ratewatch.data.RateApiException
import dev.ratewatch.data.RateRepository
import dev.ratewatch.data.resultOf
import dev.ratewatch.model.ConvertResult
import dev.ratewatch.model.Freshness
import dev.ratewatch.model.RateTable
import dev.ratewatch.model.RefreshError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * [App 3 · B7] RatesCore with a FakeRateRepository: no database, no network.
 *
 * The core runs in its own scope on the test scheduler (playing the role of the app-wide scope), so
 * `advanceUntilIdle()` drives it. The clock is a variable, so "one hour later" is one assignment.
 */
class RatesCoreTest {

    private val hour = 60 * 60 * 1000L
    private var nowMillis = 10 * hour
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
    }

    private val repo = FakeRateRepository()

    private var coreScope: CoroutineScope? = null

    // TRAP: not `backgroundScope`. advanceUntilIdle() only waits for foreground work, so launches into
    // backgroundScope would never run and every assertion would see the initial state.
    private fun TestScope.core(): RatesCore {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        coreScope = scope
        return RatesCore(repo, clock, scope)
    }

    @AfterTest
    fun stopCore() {
        coreScope?.cancel()
    }

    private fun usd(fetchedAt: Long) = RateTable("USD", mapOf("USD" to 1.0, "EUR" to 0.9, "VND" to 26_312.5), fetchedAt)

    @Test
    fun `a failed refresh keeps the saved table and shows the error`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis))
        repo.nextResult = { Result.failure(RateApiException(RefreshError.Offline)) }
        val core = core()
        advanceUntilIdle()

        core.refresh()
        advanceUntilIdle()

        val state = core.state.value
        assertEquals(RefreshError.Offline, state.error)
        assertEquals(usd(nowMillis), state.table)
        assertEquals(listOf("EUR", "VND"), state.rows.map { it.code })
        assertTrue(state.showOfflineBanner)
        assertFalse(state.refreshing)
    }

    @Test
    fun `a successful refresh shows the new table and clears the error`() = runTest {
        repo.nextResult = { Result.failure(RateApiException(RefreshError.Timeout)) }
        val core = core()
        core.refresh()
        advanceUntilIdle()
        assertTrue(core.state.value.showEmptyError)

        repo.nextResult = { base -> repo.save(usd(nowMillis), base) }
        core.refresh()
        advanceUntilIdle()

        assertNull(core.state.value.error)
        assertEquals(Freshness.Fresh, core.state.value.freshness)
    }

    @Test
    fun `refreshing turns on and off around the download`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.nextResult = { gate.await(); Result.success(Unit) }
        val core = core()
        advanceUntilIdle()

        core.state.test {
            assertFalse(awaitItem().refreshing)
            core.refresh()
            assertTrue(awaitItem().refreshing)
            gate.complete(Unit)
            assertFalse(awaitItem().refreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `start refreshes when nothing is saved`() = runTest {
        val core = core()
        core.start()
        advanceUntilIdle()
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `start does not refresh a table exactly one hour old`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis - hour))
        val core = core()
        core.start()
        advanceUntilIdle()
        assertEquals(0, repo.refreshCalls)
        assertEquals(Freshness.Fresh, core.state.value.freshness)
    }

    @Test
    fun `start refreshes a table older than one hour`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis - hour - 1))
        val core = core()
        core.start()
        advanceUntilIdle()
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `a cancelled refresh is not shown as an error`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis))
        repo.nextResult = { throw CancellationException("screen closed") }
        val core = core()
        core.refresh()
        advanceUntilIdle()
        assertNull(core.state.value.error)
        assertFalse(core.state.value.refreshing)
    }

    @Test
    fun `resultOf lets cancellation through`() = runTest {
        assertFailsWith<CancellationException> { resultOf { throw CancellationException("stop") } }
        assertTrue(resultOf { error("boom") }.isFailure)
    }

    @Test
    fun `a second refresh while one is running is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repo.nextResult = { gate.await(); Result.success(Unit) }
        val core = core()
        core.refresh()
        core.refresh()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repo.refreshCalls)
    }

    @Test
    fun `selecting a base remembers it and downloads it`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis))
        val core = core()
        core.selectBase("VND")
        advanceUntilIdle()
        assertEquals("VND", repo.base.value)
        assertEquals("VND", core.state.value.base)
        assertNull(core.state.value.table) // the USD table is never shown under the VND name
        assertEquals(listOf("VND"), repo.refreshedBases)
    }

    @Test
    fun `starring a currency moves it to the top`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis))
        val core = core()
        core.toggleFavorite("VND")
        advanceUntilIdle()
        assertEquals("VND", core.state.value.rows.first().code)
        assertTrue(core.state.value.rows.first().favorite)
    }

    @Test
    fun `convert uses the saved table and swaps both ways`() = runTest {
        repo.tables.value = mapOf("USD" to usd(fetchedAt = nowMillis))
        val core = core()
        core.onAmountChanged("2")
        advanceUntilIdle()
        assertEquals("52.625", assertIs<ConvertResult.Ready>(core.convert.value.result).amountText)

        core.swapCurrencies()
        core.onAmountChanged("26.312,5")
        advanceUntilIdle()
        assertEquals("VND", core.convert.value.input.from)
        assertEquals("1,00", assertIs<ConvertResult.Ready>(core.convert.value.result).amountText)
    }
}

/** In-memory RateRepository. [nextResult] decides what refresh() does; by default it succeeds without data. */
class FakeRateRepository : RateRepository {
    val tables = MutableStateFlow<Map<String, RateTable>>(emptyMap())
    val favorites = MutableStateFlow<Set<String>>(emptySet())
    val base = MutableStateFlow<String?>(null)
    var nextResult: suspend (base: String) -> Result<Unit> = { Result.success(Unit) }
    var refreshCalls = 0
    val refreshedBases = mutableListOf<String>()

    /** Writes [table] under [base], like the real repository's transaction. */
    fun save(table: RateTable, base: String): Result<Unit> {
        tables.value = tables.value + (base to table.copy(base = base))
        return Result.success(Unit)
    }

    override fun observe(base: String): Flow<RateTable?> = tables.map { it[base] }
    override fun observeFavorites(): Flow<Set<String>> = favorites
    override fun observeBase(): Flow<String?> = base

    override suspend fun refresh(base: String, now: Long): Result<Unit> {
        refreshCalls++
        refreshedBases += base
        return nextResult(base)
    }

    override suspend fun toggleFavorite(code: String) {
        favorites.value = if (code in favorites.value) favorites.value - code else favorites.value + code
    }

    override suspend fun setBase(code: String) {
        base.value = code
    }
}
